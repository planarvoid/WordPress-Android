package com.soundcloud.android.likes

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.Consts
import com.soundcloud.android.R
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment
import com.soundcloud.android.configuration.FeatureOperations
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment
import com.soundcloud.android.events.EventContextMetadata.builder
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.OfflineInteractionEvent
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.events.UpgradeFunnelEvent
import com.soundcloud.android.main.Screen
import com.soundcloud.android.navigation.NavigationExecutor
import com.soundcloud.android.offline.OfflineContentOperations
import com.soundcloud.android.offline.OfflineLikesDialog
import com.soundcloud.android.offline.OfflineSettingsStorage
import com.soundcloud.android.offline.OfflineState
import com.soundcloud.android.offline.OfflineStateOperations
import com.soundcloud.android.payments.UpsellContext
import com.soundcloud.android.playback.ExpandPlayerSingleObserver
import com.soundcloud.android.playback.PlaySessionSource
import com.soundcloud.android.playback.PlaybackInitiator
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.rx.observers.DefaultCompletableObserver
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.settings.OfflineStorageErrorDialog
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.subjects.BehaviorSubject
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Provider

@OpenForTesting
class TrackLikesHeaderPresenter
@Inject
constructor(private val updateHeaderObserverFactory: UpdateHeaderViewObserverFactory,
            private val offlineContentOperations: OfflineContentOperations,
            private val offlineStateOperations: OfflineStateOperations,
            private val likeOperations: TrackLikeOperations,
            private val featureOperations: FeatureOperations,
            private val playbackInitiator: PlaybackInitiator,
            private val expandPlayerObserverProvider: Provider<ExpandPlayerSingleObserver>,
            private val syncLikesDialogProvider: Provider<OfflineLikesDialog>,
            private val navigationExecutor: NavigationExecutor,
            private val eventBus: EventBusV2,
            private val offlineSettingsStorage: OfflineSettingsStorage,
            private val goOnboardingTooltipExperiment: GoOnboardingTooltipExperiment) : DefaultSupportFragmentLightCycle<Fragment>(), UpdateHeaderViewObserver.Listener, CellRenderer<TrackLikesItem> {

    private var fragment: Fragment? = null

    private val trackCountSubject: BehaviorSubject<Int> = BehaviorSubject.createDefault(Consts.NOT_SET)
    private val viewSubject: BehaviorSubject<Optional<WeakReference<View>>> = BehaviorSubject.createDefault(Optional.absent())

    private val compositeDisposables = CompositeDisposable()

    override fun onCreate(fragment: Fragment, bundle: Bundle?) {
        super.onCreate(fragment, bundle)

        val headerViewUpdateObservable = Observable.combineLatest(trackCountSubject,
                                                                  headerViewObservable(),
                                                                  getOfflineStateObservable(),
                                                                  getOfflineLikesEnabledObservable(),
                                                                  Function4<Int, WeakReference<View>, OfflineState, Boolean, HeaderViewUpdate>
                                                                  { trackCount, view, offlineState, offlineLikesEnabled ->
                                                                      HeaderViewUpdate(view,
                                                                                       trackCount,
                                                                                       featureOperations.isOfflineContentEnabled,
                                                                                       offlineLikesEnabled,
                                                                                       offlineState,
                                                                                       featureOperations.upsellOfflineContent())
                                                                  })

        compositeDisposables.add(headerViewUpdateObservable.subscribeWith(LambdaObserver.onNext(updateHeaderObserverFactory.create(this))))
    }

    override fun onViewCreated(fragment: Fragment, view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(fragment, view, savedInstanceState)
        this.fragment = fragment
    }

    override fun onDestroy(fragment: Fragment) {
        compositeDisposables.clear()
        super.onDestroy(fragment)
    }

    override fun onDestroyView(fragment: Fragment) {
        this.fragment = null
        super.onDestroyView(fragment)
    }

    override fun createItemView(parent: ViewGroup) = LayoutInflater.from(parent.context).inflate(R.layout.track_likes_header, parent, false)

    override fun bindItemView(position: Int, itemView: View, items: List<TrackLikesItem>) = viewSubject.onNext(Optional.of(WeakReference(itemView)))

    fun updateTrackCount(trackCount: Int) = trackCountSubject.onNext(trackCount)

    override fun onShuffle() {
        compositeDisposables.add(playbackInitiator.playTracksShuffled(likeOperations.likedTrackUrns(), PlaySessionSource(Screen.LIKES))
                                         .doOnEvent { _, _ ->
                                             eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffle(builder().pageName(Screen.LIKES.get())
                                                                                                               .build()))
                                         }
                                         .subscribeWith(expandPlayerObserverProvider.get()))
    }

    override fun onUpsell() {
        navigationExecutor.openUpgrade(fragment?.activity, UpsellContext.OFFLINE)
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forLikesClick())
    }

    override fun onMakeAvailableOffline(isAvailable: Boolean) = when {
        isAvailable -> enableOfflineLikes() else -> disableOfflineLikes()
    }

    private fun getOfflineLikesEnabledObservable(): Observable<Boolean> {
        return if (featureOperations.isOfflineContentEnabled) {
            offlineContentOperations.offlineLikedTracksStatusChanges.observeOn(AndroidSchedulers.mainThread())
        } else {
            Observable.just(false)
        }
    }

    private fun getOfflineStateObservable(): Observable<OfflineState> {
        return if (featureOperations.isOfflineContentEnabled) {
            eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                    .filter { event -> event.isLikedTrackCollection }
                    .map { event -> event.state }
                    .startWith(offlineStateOperations.loadLikedTracksOfflineState().toObservable())
                    .observeOn(AndroidSchedulers.mainThread())
        } else {
            Observable.just(OfflineState.NOT_OFFLINE)
        }
    }

    private fun enableOfflineLikes() {
        if (offlineSettingsStorage.isOfflineContentAccessible) {
            handleEnableOfflineLikes()
        } else {
            OfflineStorageErrorDialog.show(fragment?.fragmentManager)
        }
    }

    private fun handleEnableOfflineLikes() {
        if (goOnboardingTooltipExperiment.isEnabled) {
            offlineContentOperations.enableOfflineLikedTracks().subscribe(DefaultCompletableObserver())
        } else {
            syncLikesDialogProvider.get().show(fragment?.fragmentManager)
        }
    }

    private fun disableOfflineLikes() {
        if (offlineContentOperations.isOfflineCollectionEnabled) {
            ConfirmRemoveOfflineDialogFragment.showForLikes(fragment?.fragmentManager)
        } else {
            offlineContentOperations.disableOfflineLikedTracks().subscribe(DefaultCompletableObserver())
            eventBus.publish(EventQueue.TRACKING,
                             OfflineInteractionEvent.fromRemoveOfflineLikes(Screen.LIKES.get()))
        }
    }

    private fun headerViewObservable() =
            viewSubject
                    .filter { it.isPresent }
                    .map { it.get() }

}

