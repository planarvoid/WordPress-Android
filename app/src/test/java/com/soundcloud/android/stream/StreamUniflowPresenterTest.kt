package com.soundcloud.android.stream

import android.content.Context
import android.view.TextureView
import android.view.View
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.ads.AdFixtures
import com.soundcloud.android.ads.AdItemCallback
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.ads.WhyAdsDialogPresenter
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.FacebookInvitesEvent
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.events.UpgradeFunnelEvent
import com.soundcloud.android.facebookinvites.FacebookNotificationCallback
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationTarget
import com.soundcloud.android.navigation.Navigator
import com.soundcloud.android.payments.UpsellContext
import com.soundcloud.android.playback.TrackSourceInfo
import com.soundcloud.android.playback.VideoSurfaceProvider
import com.soundcloud.android.rx.RxSignal
import com.soundcloud.android.upsell.InlineUpsellOperations
import com.soundcloud.android.upsell.UpsellItemCallback
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.TestEventBusV2
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StreamUniflowPresenterTest {

    @Mock private lateinit var streamOperations: StreamUniflowOperations
    @Mock private lateinit var navigator: Navigator
    @Mock private lateinit var whyAdsDialogPresenter: WhyAdsDialogPresenter
    @Mock private lateinit var streamAdsController: StreamAdsController
    @Mock private lateinit var videoSurfaceProvider: VideoSurfaceProvider
    @Mock private lateinit var upsellOperations: InlineUpsellOperations
    @Mock private lateinit var view: StreamUniflowView
    @Mock private lateinit var context: Context

    private val eventBus = TestEventBusV2()
    private val requestContentSubject = PublishSubject.create<RxSignal>()
    private val refreshSignalSubject = PublishSubject.create<RxSignal>()
    private val facebookListenerInvitesSubject = PublishSubject.create<FacebookNotificationCallback<StreamItem.FacebookListenerInvites>>()
    private val facebookCreatorInvitesSubject = PublishSubject.create<FacebookNotificationCallback<StreamItem.FacebookCreatorInvites>>()
    private val upsellLoadingResultSubject = PublishSubject.create<UpsellItemCallback>()
    private val videoAdItemClickSubject = PublishSubject.create<AdItemCallback>()
    private val appInstallClick = PublishSubject.create<AdItemCallback>()
    private lateinit var presenter: StreamUniflowPresenter

    @Before
    fun setUp() {
        whenever(view.requestContent()).thenReturn(requestContentSubject)
        whenever(view.refreshSignal()).thenReturn(refreshSignalSubject)
        whenever(view.facebookListenerInvitesItemCallback()).thenReturn(facebookListenerInvitesSubject)
        whenever(view.facebookCreatorInvitesItemCallback()).thenReturn(facebookCreatorInvitesSubject)
        whenever(view.upsellItemCallback()).thenReturn(upsellLoadingResultSubject)
        whenever(view.videoAdItemCallback()).thenReturn(videoAdItemClickSubject)
        whenever(view.appInstallCallback()).thenReturn(appInstallClick)
        presenter = StreamUniflowPresenter(streamOperations, eventBus, navigator, whyAdsDialogPresenter, streamAdsController, videoSurfaceProvider, upsellOperations)
        presenter.attachView(view)
    }

    @Test
    fun `tracks facebook listener invites click and shows response`() {
        facebookListenerInvitesSubject.onNext(FacebookNotificationCallback.Click(0, StreamItem.FacebookListenerInvites(Optional.absent())))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(FacebookInvitesEvent.forListenerClick(false))

        verify(view).showFacebookListenersInvitesDialog()
    }

    @Test
    fun `tracks facebook listener invites dismiss event`() {
        facebookListenerInvitesSubject.onNext(FacebookNotificationCallback.Dismiss(0, StreamItem.FacebookListenerInvites(Optional.absent())))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(FacebookInvitesEvent.forListenerDismiss(false))
    }

    @Test
    fun `tracks facebook listener invites loaded event`() {
        val hasPictures = false
        facebookListenerInvitesSubject.onNext(FacebookNotificationCallback.Load(hasPictures))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(FacebookInvitesEvent.forListenerShown(hasPictures))
    }

    @Test
    fun `tracks facebook creator invites click and shows response when urn is set`() {
        val trackUrn = Urn.forTrack(1L)
        val trackUrl = "trackUrl"
        facebookCreatorInvitesSubject.onNext(FacebookNotificationCallback.Click(0, StreamItem.FacebookCreatorInvites(trackUrn, trackUrl)))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(FacebookInvitesEvent.forCreatorClick())

        verify(view).showFacebookCreatorsInvitesDialog(trackUrl, trackUrn)
    }

    @Test
    fun `tracks facebook creator invites dismiss event`() {
        facebookCreatorInvitesSubject.onNext(FacebookNotificationCallback.Dismiss(0, StreamItem.FacebookCreatorInvites(Urn.forTrack(1L), "trackUrl")))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(FacebookInvitesEvent.forCreatorDismiss())
    }

    @Test
    fun `on upsell item dismiss disables upsell in stream`() {
        upsellLoadingResultSubject.onNext(UpsellItemCallback.Dismiss(0))

        verify(upsellOperations).disableInStream()
    }

    @Test
    fun `on upsell item click navigates to upgrade and tracks`() {
        upsellLoadingResultSubject.onNext(UpsellItemCallback.Click(context))

        verify(navigator).navigateTo(NavigationTarget.forUpgrade(UpsellContext.PREMIUM_CONTENT))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(UpgradeFunnelEvent.forStreamClick())
    }

    @Test
    fun `tracks upsell item created event`() {
        upsellLoadingResultSubject.onNext(UpsellItemCallback.Create)

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(UpgradeFunnelEvent.forStreamImpression())
    }

    @Test
    fun `tracks video ad click and navigates to url`() {
        val adData = AdFixtures.getInlayVideoAd(0L)
        videoAdItemClickSubject.onNext(AdItemCallback.AdItemClick(adData))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(UIEvent.fromPlayableClickThrough(adData, TrackSourceInfo(Screen.STREAM.get(), true)))
        verify(navigator).navigateTo(NavigationTarget.forAdClickthrough(adData.clickThroughUrl()))
    }

    @Test
    fun `on video texture bind sets texture view to provider when not in full screen`() {
        val adData = AdFixtures.getInlayVideoAd(0L)
        whenever(streamAdsController.isInFullscreen).thenReturn(false)
        val textureView = mock<TextureView>()
        val viewabilityLayer = mock<View>()
        videoAdItemClickSubject.onNext(AdItemCallback.VideoTextureBind(textureView, viewabilityLayer, adData))

        verify(videoSurfaceProvider).setTextureView(adData.uuid(), VideoSurfaceProvider.Origin.STREAM, textureView, viewabilityLayer)
    }

    @Test
    fun `on video texture bind does not set texture view to provider when in full screen`() {
        whenever(streamAdsController.isInFullscreen).thenReturn(true)
        videoAdItemClickSubject.onNext(AdItemCallback.VideoTextureBind(mock(), mock(), AdFixtures.getInlayVideoAd(0L)))

        verifyZeroInteractions(videoSurfaceProvider)
    }

    @Test
    fun `on video fullscreen click enables fullscreen and navigates to fullscreen ad`() {
        val adData = AdFixtures.getInlayVideoAd(0L)
        videoAdItemClickSubject.onNext(AdItemCallback.VideoFullscreenClick(adData))

        verify(streamAdsController).setFullscreenEnabled()
        verify(navigator).navigateTo(NavigationTarget.forFullscreenVideoAd(adData.adUrn()))
    }

    @Test
    fun `shows why video ad dialog`() {
        videoAdItemClickSubject.onNext(AdItemCallback.WhyAdsClicked(context))

        verify(whyAdsDialogPresenter).show(context)
    }

    @Test
    fun `tracks app install ad click and navigates to url`() {
        val adData = AdFixtures.getApiAppInstallAd()
        appInstallClick.onNext(AdItemCallback.AdItemClick(adData))

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isEqualTo(UIEvent.fromAppInstallAdClickThrough(adData))
        verify(navigator).navigateTo(NavigationTarget.forAdClickthrough(adData.clickThroughUrl()))
    }

    @Test
    fun `shows why app install ad dialog`() {
        appInstallClick.onNext(AdItemCallback.WhyAdsClicked(context))

        verify(whyAdsDialogPresenter).show(context)
    }
}
