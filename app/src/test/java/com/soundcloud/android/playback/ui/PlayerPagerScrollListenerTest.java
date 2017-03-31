package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioAdQueueItem;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playback.ui.view.PlayerTrackPager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestObserver;

import android.support.v4.view.ViewPager;

// AndroidUnitTest because underlying audio ads use Uri.parse()
public class PlayerPagerScrollListenerTest extends AndroidUnitTest {

    @Mock PlayQueueManager playQueueManager;
    @Mock PlayerTrackPager playerTrackPager;
    @Mock PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock AdsOperations adsOperations;
    @Mock PlayerPagerPresenter presenter;

    private PlayerPagerScrollListener pagerScrollListener;
    private TestObserver<Integer> observer;

    private PlayQueueItem playQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(1));

    @Before
    public void setUp() {
        observer = new TestObserver<>();
        pagerScrollListener = new PlayerPagerScrollListener(playQueueManager, playbackFeedbackHelper, adsOperations);
        pagerScrollListener.initialize(playerTrackPager, presenter);
        pagerScrollListener.getPageChangedObservable().subscribe(observer);
    }

    @Test
    public void doesNotEmitTrackChangedOnIdleStateWithoutPageSelected() {
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        assertThat(observer.getOnNextEvents()).isEmpty();
    }

    @Test
    public void doesNotEmitTrackChangedOnIdleStateWithoutPrecedingPageSelected() {
        when(presenter.getItemAtPosition(2)).thenReturn(playQueueItem);
        pagerScrollListener.onPageSelected(2);
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void showsBlockedSwipeFeedbackWhenSwipeOnAdPage() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);

        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        verify(playbackFeedbackHelper).showUnskippableAdFeedback();
    }

    @Test
    public void doesNotShowBlocksSwipeFeedbackWhenSwipeOnTrackPage() {
        pagerScrollListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);

        verify(playbackFeedbackHelper, never()).showUnskippableAdFeedback();
    }

    @Test
    public void setsPagingDisabledOnPageSelectedWithAdNotAtCurrentPosition() {
        final AudioAd audioAd = AdFixtures.getAudioAd(Urn.forTrack(2));
        final AudioAdQueueItem item = TestPlayQueueItem.createAudioAd(audioAd);
        when(presenter.getItemAtPosition(1)).thenReturn(item);

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(false);
    }

    @Test
    public void setsPagingEnabledOnPageSelectedWithAdAtCurrentPosition() {
        final PlayQueueItem item = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(2)));
        when(presenter.getItemAtPosition(1)).thenReturn(item);
        when(playQueueManager.isCurrentItem(item)).thenReturn(true);

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(true);
    }

    @Test
    public void setsPagingEnabledOnPageSelectedWithCurrentNormalTrack() {
        when(presenter.getItemAtPosition(1)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(1)));

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(true);
    }

    @Test
    public void setsPagingEnabledOnPageSelectedWithNormalTrack() {
        // for normal tracks paging should always be enabled
        when(presenter.getItemAtPosition(1)).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(1)));

        pagerScrollListener.onPageSelected(1);

        verify(playerTrackPager).setPagingEnabled(true);
    }

}
