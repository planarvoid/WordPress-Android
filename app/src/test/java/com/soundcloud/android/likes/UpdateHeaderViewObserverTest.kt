package com.soundcloud.android.likes

import android.view.LayoutInflater
import android.view.View
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.R
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter
import com.soundcloud.android.offline.OfflineSettingsOperations
import com.soundcloud.android.offline.OfflineState
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.utils.ConnectionHelper
import com.soundcloud.rx.eventbus.TestEventBusV2
import kotlinx.android.synthetic.main.downloadable_header.view.*
import kotlinx.android.synthetic.main.track_likes_header.view.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.robolectric.RuntimeEnvironment
import java.lang.ref.WeakReference

class UpdateHeaderViewObserverTest : AndroidUnitTest() {

    private lateinit var observer: UpdateHeaderViewObserver

    @Mock private lateinit var offlineSettings: OfflineSettingsOperations
    @Mock private lateinit var connectionHelper: ConnectionHelper
    @Mock private lateinit var tooltipExperiment: GoOnboardingTooltipExperiment
    @Mock private lateinit var overlayPresenter: IntroductoryOverlayPresenter
    @Mock private lateinit var listener: UpdateHeaderViewObserver.Listener
    private lateinit var headerViewRef: WeakReference<View>

    private val headerView = LayoutInflater.from(context()).inflate(R.layout.track_likes_header, null)

    @Before
    fun setUp() {
        observer = UpdateHeaderViewObserver(
                offlineSettings,
                connectionHelper,
                TestEventBusV2(),
                tooltipExperiment,
                AndroidUnitTest.resources(),
                overlayPresenter,
                listener
        )

        headerViewRef = WeakReference(headerView)
    }

    @Test
    fun `handles missing view`() {
        observer.accept(createHeaderViewUpdate(view = WeakReference<View>(null)))
    }

    @Test
    fun `hides view when no tracks`() {
        observer.accept(createHeaderViewUpdate(trackCount = 0))

        assertThat(headerView.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun `populates view with 1 item`() {
        observer.accept(createHeaderViewUpdate(trackCount = 1))

        with(headerView) {
            assertThat(shuffle_btn.isEnabled).isFalse()
            assertThat(shuffle_btn.visibility).isEqualTo(View.GONE)
            assertThat(visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `populates view with 2 items`() {
        observer.accept(createHeaderViewUpdate(trackCount = 2))

        with(headerView) {
            assertThat(shuffle_btn.isEnabled).isTrue()
            assertThat(shuffle_btn.visibility).isEqualTo(View.VISIBLE)
            assertThat(visibility).isEqualTo(View.VISIBLE)
            assertThat(header_text.text).isEqualTo(RuntimeEnvironment.application.resources.getQuantityString(R.plurals.number_of_liked_tracks_you_liked, 2, 2))
        }
    }

    @Test
    fun `shuffle listener clicked`() {
        observer.accept(createHeaderViewUpdate(trackCount = 2))

        headerView.shuffle_btn.callOnClick()

        verify(listener).onShuffle()
    }

    @Test
    fun `offline state requested`() {
        whenever(offlineSettings.isWifiOnlyEnabled).thenReturn(true)
        whenever(connectionHelper.isWifiConnected).thenReturn(false)

        observer.accept(createHeaderViewUpdate(offlineState = OfflineState.REQUESTED, isOfflineContentEnabled = true))

        with(headerView.offline_state_button) {
            assertThat(visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `shows introductory overlay`() {
        whenever(tooltipExperiment.isEnabled).thenReturn(true)
        whenever(connectionHelper.isWifiConnected).thenReturn(true)
        whenever(connectionHelper.isNetworkConnected).thenReturn(true)

        observer.accept(createHeaderViewUpdate(isOfflineLikesEnabled = false, isOfflineContentEnabled = true))

        verify(overlayPresenter).showIfNeeded(any())
    }

    private fun createHeaderViewUpdate(view: WeakReference<View> = headerViewRef,
                                       trackCount: Int = 1,
                                       isOfflineContentEnabled: Boolean = false,
                                       isOfflineLikesEnabled: Boolean = false,
                                       offlineState: OfflineState = OfflineState.NOT_OFFLINE,
                                       upsellOfflineContent: Boolean = false): HeaderViewUpdate =
            HeaderViewUpdate(view, trackCount, isOfflineContentEnabled, isOfflineLikesEnabled, offlineState, upsellOfflineContent)
}
