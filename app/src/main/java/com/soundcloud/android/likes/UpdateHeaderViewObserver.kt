package com.soundcloud.android.likes

import android.content.res.Resources
import android.view.View
import com.google.auto.factory.Provided
import com.soundcloud.android.R
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.GoOnboardingTooltipEvent
import com.soundcloud.android.events.TrackingEvent
import com.soundcloud.android.events.UpgradeFunnelEvent
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlay
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter
import com.soundcloud.android.offline.OfflineSettingsOperations
import com.soundcloud.android.offline.OfflineState
import com.soundcloud.android.utils.ConnectionHelper
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.downloadable_header.view.*
import kotlinx.android.synthetic.main.track_likes_header.view.*
import java.lang.ref.WeakReference
import javax.inject.Inject

@OpenForTesting
class UpdateHeaderViewObserver(@param:Provided private val offlineSettings: OfflineSettingsOperations,
                               @param:Provided private val connectionHelper: ConnectionHelper,
                               @param:Provided private val eventBus: EventBusV2,
                               @param:Provided private val goOnboardingTooltipExperiment: GoOnboardingTooltipExperiment,
                               @param:Provided private val resources: Resources,
                               @param:Provided private val introductoryOverlayPresenter: IntroductoryOverlayPresenter,
                               private val listener: Listener) : Consumer<HeaderViewUpdate> {

    private var previousUpdate: Optional<HeaderViewUpdate> = Optional.absent()

    interface Listener {
        fun onShuffle()

        fun onUpsell()

        fun onMakeAvailableOffline(isAvailable: Boolean)
    }

    override fun accept(headerViewUpdate: HeaderViewUpdate) {
        val headerView = headerViewUpdate.view.get()
        if (headerView != null) {
            render(headerView, headerViewUpdate)
        }
    }

    private fun render(headerView: View, headerViewUpdate: HeaderViewUpdate) {

        if (headerViewUpdate.trackCount == 0) {
            headerView.visibility = View.GONE
        } else {
            populateHeaderView(headerView, headerViewUpdate)
            headerView.visibility = View.VISIBLE
        }
        previousUpdate = Optional.of<HeaderViewUpdate>(headerViewUpdate)
    }

    private fun populateHeaderView(headerView: View, headerViewUpdate: HeaderViewUpdate) {

        headerView.header_text.text = resources.getQuantityString(R.plurals.number_of_liked_tracks_you_liked, headerViewUpdate.trackCount, headerViewUpdate.trackCount)

        with(headerView.shuffle_btn) {
            setOnClickListener { listener.onShuffle() }
            visibility = if (headerViewUpdate.trackCount <= 1) View.GONE else View.VISIBLE
            isEnabled = headerViewUpdate.trackCount > 1
        }

        configureOfflineState(headerViewUpdate, headerView)
    }

    private fun configureOfflineState(headerViewUpdate: HeaderViewUpdate,
                                      view: View) {

        when {
            headerViewUpdate.isOfflineContentEnabled -> configureForOfflineEnabled(view, headerViewUpdate)
            upsellOfflineContentChanged(headerViewUpdate) -> configureForUpsell(view)
            else -> view.offline_state_button.setState(OfflineState.NOT_OFFLINE)
        }
    }

    private fun configureForOfflineEnabled(view: View,
                                           headerViewUpdate: HeaderViewUpdate) {
        with(view.offline_state_button) {
            visibility = View.VISIBLE
            setOnClickListener { listener.onMakeAvailableOffline(!headerViewUpdate.isOfflineLikesEnabled) }
            setState(headerViewUpdate.offlineState)

            if (headerViewUpdate.offlineState == OfflineState.REQUESTED) {
                if (offlineSettings.isWifiOnlyEnabled && !connectionHelper.isWifiConnected) {
                    showNoWiFi()
                } else if (!connectionHelper.isNetworkConnected) {
                    showNoConnection()
                }
            }
        }

        if (shouldShowOfflineIntroductoryOverlay(headerViewUpdate.isOfflineLikesEnabled) && canSyncOfflineOnCurrentConnection()) {
            introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlay.builder()
                                                              .overlayKey(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES)
                                                              .targetView(view.offline_state_button)
                                                              .title(R.string.overlay_listen_offline_likes_title)
                                                              .description(R.string.overlay_listen_offline_likes_description)
                                                              .event(Optional.of(GoOnboardingTooltipEvent.forListenOfflineLikes()))
                                                              .build())
        }
    }

    private fun configureForUpsell(view: View) {
        view.offline_state_button.visibility = View.VISIBLE
        view.offline_state_button.setOnClickListener { listener.onUpsell() }
        eventBus.publish<TrackingEvent>(EventQueue.TRACKING, UpgradeFunnelEvent.forLikesImpression())
    }

    private fun upsellOfflineContentChanged(headerViewUpdate: HeaderViewUpdate): Boolean {
        return if (previousUpdate.isPresent) {
            previousUpdate.get().upsellOfflineContent != headerViewUpdate.upsellOfflineContent
        } else {
            headerViewUpdate.upsellOfflineContent
        }
    }

    private fun shouldShowOfflineIntroductoryOverlay(offlineLikesEnabled: Boolean): Boolean = !offlineLikesEnabled && goOnboardingTooltipExperiment.isEnabled

    private fun canSyncOfflineOnCurrentConnection(): Boolean = if (offlineSettings.isWifiOnlyEnabled) connectionHelper.isWifiConnected else connectionHelper.isNetworkConnected

}

@OpenForTesting
class UpdateHeaderViewObserverFactory
@Inject constructor(private val offlineSettings: OfflineSettingsOperations,
                    private val connectionHelper: ConnectionHelper,
                    private val eventBus: EventBusV2,
                    private val goOnboardingTooltipExperiment: GoOnboardingTooltipExperiment,
                    private val resources: Resources,
                    private val introductoryOverlayPresenter: IntroductoryOverlayPresenter) {

    fun create(listener: UpdateHeaderViewObserver.Listener): UpdateHeaderViewObserver =
            UpdateHeaderViewObserver(offlineSettings, connectionHelper, eventBus, goOnboardingTooltipExperiment, resources, introductoryOverlayPresenter, listener)
}

data class HeaderViewUpdate(val view: WeakReference<View>,
                            val trackCount: Int,
                            val isOfflineContentEnabled: Boolean,
                            val isOfflineLikesEnabled: Boolean,
                            val offlineState: OfflineState,
                            val upsellOfflineContent: Boolean)
