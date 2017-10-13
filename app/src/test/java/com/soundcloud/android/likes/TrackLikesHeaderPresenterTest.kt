package com.soundcloud.android.likes

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.view.View
import android.widget.ListView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.Consts
import com.soundcloud.android.configuration.FeatureOperations
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.OfflineInteractionEvent
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationExecutor
import com.soundcloud.android.offline.OfflineContentChangedEvent.requested
import com.soundcloud.android.offline.OfflineContentOperations
import com.soundcloud.android.offline.OfflineLikesDialog
import com.soundcloud.android.offline.OfflineSettingsOperations
import com.soundcloud.android.offline.OfflineSettingsStorage
import com.soundcloud.android.offline.OfflineState
import com.soundcloud.android.offline.OfflineStateOperations
import com.soundcloud.android.payments.UpsellContext
import com.soundcloud.android.playback.PlaySessionSource
import com.soundcloud.android.playback.PlaybackInitiator
import com.soundcloud.android.playback.PlaybackResult
import com.soundcloud.android.presentation.ListItemAdapter
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.testsupport.InjectionSupport
import com.soundcloud.android.testsupport.fixtures.TestSubscribers
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.android.utils.ConnectionHelper
import com.soundcloud.rx.eventbus.TestEventBusV2
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import java.util.Arrays.asList
import java.util.Collections
import java.util.Collections.singletonList

class TrackLikesHeaderPresenterTest : AndroidUnitTest() {

    @Mock private lateinit var updateHeaderViewObserverFactory: UpdateHeaderViewObserverFactory
    @Mock private lateinit var observer: UpdateHeaderViewObserver
    @Mock private lateinit var offlineContentOperations: OfflineContentOperations
    @Mock private lateinit var offlineStateOperations: OfflineStateOperations
    @Mock private lateinit var likeOperations: TrackLikeOperations
    @Mock private lateinit var featureOperations: FeatureOperations
    @Mock private lateinit var playbackInitiator: PlaybackInitiator
    @Mock private lateinit var offlineLikesDialog: OfflineLikesDialog
    @Mock private lateinit var connectionHelper: ConnectionHelper
    @Mock private lateinit var offlineSettings: OfflineSettingsOperations
    @Mock private lateinit var navigationExecutor: NavigationExecutor
    @Mock private lateinit var offlineSettingsStorage: OfflineSettingsStorage
    @Mock private lateinit var adapter: ListItemAdapter<TrackItem>
    @Mock private lateinit var fragment: Fragment
    @Mock private lateinit var layoutView: View
    @Mock private lateinit var listView: ListView
    @Mock private lateinit var fragmentManager: FragmentManager
    @Mock private lateinit var fragmentTransaction: FragmentTransaction
    @Mock private lateinit var goOnboardingTooltipExperiment: GoOnboardingTooltipExperiment

    private lateinit var presenter: TrackLikesHeaderPresenter
    private var eventBus: TestEventBusV2 = TestEventBusV2()
    private var likedTrackUrns: List<Urn> = asList(TRACK1, TRACK2)

    private val offlineLikesSubject = BehaviorSubject.createDefault(true)

    @Before
    @Throws(Exception::class)
    fun setUp() {
        whenever(updateHeaderViewObserverFactory.create(any())).thenReturn(observer)
        whenever(fragmentManager.beginTransaction()).thenReturn(fragmentTransaction)
        whenever(fragment.activity).thenReturn(AndroidUnitTest.activity())
        whenever(fragment.fragmentManager).thenReturn(fragmentManager)
        whenever(featureOperations.isOfflineContentEnabled).thenReturn(true)
        whenever(offlineStateOperations.loadLikedTracksOfflineState()).thenReturn(Single.just(OfflineState.NOT_OFFLINE))
        whenever(offlineContentOperations.offlineLikedTracksStatusChanges).thenReturn(offlineLikesSubject)
        whenever(connectionHelper.isNetworkConnected).thenReturn(true)
        whenever(offlineSettingsStorage.isOfflineContentAccessible).thenReturn(true)
        presenter = TrackLikesHeaderPresenter(
                updateHeaderViewObserverFactory,
                offlineContentOperations,
                offlineStateOperations,
                likeOperations,
                featureOperations,
                playbackInitiator,
                TestSubscribers.expandPlayerObserver(eventBus),
                InjectionSupport.providerOf(offlineLikesDialog),
                navigationExecutor,
                eventBus,
                offlineSettingsStorage,
                goOnboardingTooltipExperiment)
    }

    @Test
    fun `emit tracking event on shuffle button click`() {
        val likedTrackUrns = Single.just(likedTrackUrns)
        whenever(likeOperations.likedTrackUrns()).thenReturn(likedTrackUrns)
        whenever(playbackInitiator.playTracksShuffled(eq(likedTrackUrns), ArgumentMatchers.any(PlaySessionSource::class.java)))
                .thenReturn(Single.just(PlaybackResult.success()))
        createAndBindView()

        presenter.onShuffle()

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).kind).isEqualTo(UIEvent.Kind.SHUFFLE.toString())
    }

    @Test
    fun `enables offline likes without dialog when experiment enabled`() {
        whenever(offlineContentOperations.enableOfflineLikedTracks()).thenReturn(Completable.complete())
        whenever(goOnboardingTooltipExperiment.isEnabled).thenReturn(true)

        createAndBindView()

        presenter.onMakeAvailableOffline(true)

        verify(offlineContentOperations).enableOfflineLikedTracks()
        verifyZeroInteractions(offlineLikesDialog)
    }

    @Test
    fun `shows sync likes dialog when offline likes enabled`() {
        createAndBindView()

        presenter.onMakeAvailableOffline(true)

        verify(offlineLikesDialog).show(ArgumentMatchers.any())
    }

    @Test
    fun `disables likes syncing when offline likes disabled`() {
        whenever(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(Completable.complete())
        createAndBindView()

        presenter.onMakeAvailableOffline(false)

        verify(offlineContentOperations).disableOfflineLikedTracks()
        verifyZeroInteractions(offlineLikesDialog)
    }

    @Test
    fun `opens upgrade flow on upsell click`() {
        presenter.onViewCreated(fragment, layoutView, null)
        presenter.bindItemView(0, layoutView, emptyList())

        presenter.onUpsell()

        verify(navigationExecutor).openUpgrade(ArgumentMatchers.any(), eq(UpsellContext.OFFLINE))
    }

    @Test
    fun `handles offline content not accessible`() {
        whenever(offlineSettingsStorage.isOfflineContentAccessible).thenReturn(false)

        createAndBindView()
        presenter.onMakeAvailableOffline(true)

        verify(fragmentTransaction).add(ArgumentMatchers.any(), anyString())
        verify(fragmentTransaction).commit()
        verify(offlineLikesDialog, never()).show(ArgumentMatchers.any())
    }

    @Test
    fun `sends tracking event when removing offline likes`() {
        whenever(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(Completable.never())
        whenever(offlineContentOperations.isOfflineCollectionEnabled).thenReturn(false)
        createAndBindView()

        presenter.onMakeAvailableOffline(false)

        val trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent::class.java)
        assertThat(trackingEvent.offlineContentContext().get()).isEqualTo(OfflineInteractionEvent.OfflineContentContext.LIKES_CONTEXT)
        assertThat(trackingEvent.isEnabled.get()).isEqualTo(false)
        assertThat(trackingEvent.clickName().get()).isEqualTo(OfflineInteractionEvent.Kind.KIND_OFFLINE_LIKES_REMOVE)
        assertThat(trackingEvent.pageName().get()).isEqualTo(Screen.LIKES.get())
    }

    @Test
    fun `emits view after binding`() {
        createAndBindView()

        verifyLatestEmission()
    }

    @Test
    fun `emits new view`() {
        createAndBindView()

        val newView = mock<View>()

        presenter.bindItemView(0, newView, Collections.emptyList());

        verifyLatestEmission(view = newView)
    }

    @Test
    fun `emits with new track count`() {
        createAndBindView()

        val newTrackCount = 5
        presenter.updateTrackCount(newTrackCount)

        verifyLatestEmission(trackCount = newTrackCount)
    }

    @Test
    fun `emits with offline likes disabled`() {
        createAndBindView()

        offlineLikesSubject.onNext(false)

        verifyLatestEmission(offlineLikesEnabled = false)
    }

    @Test
    fun `emits with offline content disabled`() {
        whenever(featureOperations.isOfflineContentEnabled).thenReturn(false)

        createAndBindView()

        verifyLatestEmission(offlineContentEnabled = false, offlineLikesEnabled = false)
    }

    @Test
    fun `emits with upsell offline content enabled`() {
        whenever(featureOperations.upsellOfflineContent()).thenReturn(true)

        createAndBindView()

        verifyLatestEmission(upsellOfflineContent = true)
    }

    @Test
    fun `emits with offline state`() {
        createAndBindView()

        val offlineState = OfflineState.REQUESTED
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(TRACK1), true))

        verifyLatestEmission(offlineState = offlineState)
    }

    @Test
    fun `emits all updates`() {
        createAndBindView()

        val newView = mock<View>()

        presenter.bindItemView(0, newView, Collections.emptyList());

        val offlineState = OfflineState.REQUESTED
        eventBus.publish(EventQueue.OFFLINE_CONTENT_CHANGED, requested(singletonList(TRACK1), true))

        val newTrackCount = 5
        presenter.updateTrackCount(newTrackCount)

        offlineLikesSubject.onNext(false)

        verifyLatestEmission(view = newView, offlineState = offlineState, trackCount = newTrackCount, offlineLikesEnabled = false)
    }

    private fun verifyLatestEmission(view: View = layoutView,
                                     trackCount: Int = Consts.NOT_SET,
                                     offlineContentEnabled: Boolean = true,
                                     offlineLikesEnabled: Boolean = true,
                                     upsellOfflineContent: Boolean = false,
                                     offlineState: OfflineState = OfflineState.NOT_OFFLINE) {

        argumentCaptor<HeaderViewUpdate>().apply {
            verify(observer, atLeastOnce()).accept(capture())

            val lastUpdate = allValues[allValues.size - 1]
            assertThat(lastUpdate.view.get()).isSameAs(view)
            assertThat(lastUpdate.trackCount).isEqualTo(trackCount)
            assertThat(lastUpdate.isOfflineContentEnabled).isEqualTo(offlineContentEnabled)
            assertThat(lastUpdate.isOfflineLikesEnabled).isEqualTo(offlineLikesEnabled)
            assertThat(lastUpdate.upsellOfflineContent).isEqualTo(upsellOfflineContent)
            assertThat(lastUpdate.offlineState).isEqualTo(offlineState)

        }
    }

    private fun createAndBindView() {
        presenter.onCreate(fragment, null)
        presenter.onViewCreated(fragment, layoutView, null)
        presenter.bindItemView(0, layoutView, emptyList())
    }

    companion object {
        private val TRACK1 = Urn.forTrack(123L)
        private val TRACK2 = Urn.forTrack(456L)
    }
}
