package com.soundcloud.android.playback.ui;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.res.Resources;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

@RunWith(SoundCloudTestRunner.class)
public class PlayerPresenterTest {

    private PlayerPresenter playerPresenter;
    private ViewPager.OnPageChangeListener pagerListener;

    @Mock
    private TrackPagerAdapter trackPagerAdapter;
    @Mock
    private PlayerListener listener;
    @Mock
    private ViewPager trackPager;
    @Mock
    private View view;
    @Mock
    private Resources resources;
    @Mock
    private Button playButton;
    @Mock
    private Button nextButton;
    @Mock
    private Button previousButton;

    @Before
    public void setUp() throws Exception {
        ArgumentCaptor<ViewPager.OnPageChangeListener> captor = ArgumentCaptor.forClass(ViewPager.OnPageChangeListener.class);
        when(view.findViewById(R.id.player_play)).thenReturn(playButton);
        when(view.findViewById(R.id.player_track_pager)).thenReturn(trackPager);
        when(view.findViewById(R.id.player_next)).thenReturn(nextButton);
        when(view.findViewById(R.id.player_previous)).thenReturn(previousButton);
        playerPresenter = new PlayerPresenter(resources, trackPagerAdapter, listener, view);
        verify(trackPager).setOnPageChangeListener(captor.capture());
        pagerListener = captor.getValue();
    }

    @Test
    public void onPageSelectedOnTrackChangeListenerCallsOnTrackChangedOnListener() {
        pagerListener.onPageSelected(3);
        verify(listener).onTrackChanged(3);
    }

    @Test
    public void setQueuePositionCallsSetCurrentItemOnTrackPager() {
        playerPresenter.setQueuePosition(2);
        verify(trackPager).setCurrentItem(eq(2), anyBoolean());
    }

    @Test
    public void setQueuePositionAnimatesToAdjacentTracks() throws Exception {
        when(trackPager.getCurrentItem()).thenReturn(2);

        playerPresenter.setQueuePosition(3);

        verify(trackPager).setCurrentItem(3, true);
    }

    @Test
    public void setQueuePositionDoesNotAnimateToNotAdjacentTracks() throws Exception {
        when(trackPager.getCurrentItem()).thenReturn(2);

        playerPresenter.setQueuePosition(4);

        verify(trackPager).setCurrentItem(4, false);
    }

    @Test
    public void onPlayQueueChangedNotifiesDataSetChangedOnAdapter() {
        playerPresenter.onPlayQueueChanged();
        verify(trackPagerAdapter).notifyDataSetChanged();
    }

    @Test
    public void onPlayQueueChangedSetsTrackPagerAdapterIfNotSet() {
        playerPresenter.onPlayQueueChanged();
        verify(trackPager).setAdapter(trackPagerAdapter);
    }

    @Test
    public void onPlayStateChangedNotifiesTrackPageAdapter() {
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE);
        playerPresenter.onPlayStateChanged(stateTransition);
        verify(trackPagerAdapter).setPlayState(stateTransition);
    }

    @Test
    public void onPlayerProgressSetsCurrentProgressOnTrackAdapter() {
        PlaybackProgress progressEvent = new PlaybackProgress(5l, 10l);
        playerPresenter.onPlayerProgress(progressEvent);
        verify(trackPagerAdapter).setProgressOnCurrentTrack(progressEvent);
    }

    @Test
    public void presenterFactoryCreatesPresenterWithTrackPagerFromConstructor() {
        reset(trackPager);
        PlayerPresenter presenter = new PlayerPresenter.Factory(resources, trackPagerAdapter, listener).create(view);

        presenter.onPlayQueueChanged();

        verify(trackPager).setAdapter(trackPagerAdapter);
    }

}