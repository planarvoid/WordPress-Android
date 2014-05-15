package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ToggleButton;

@RunWith(SoundCloudTestRunner.class)
public class PlayerPresenterTest {

    private PlayerPresenter playerPresenter;
    private ViewPager.OnPageChangeListener pagerListener;

    @Mock
    private TrackPagerAdapter trackPagerAdapter;
    @Mock
    private PlayerPresenter.Listener listener;
    @Mock
    private ViewPager trackPager;
    @Mock
    private View view;
    @Mock
    private ToggleButton footerToggle;
    @Mock
    private ToggleButton playerToggle;
    @Mock
    private View nextButton;
    @Mock
    private View previousButton;

    @Before
    public void setUp() throws Exception {
        ArgumentCaptor<ViewPager.OnPageChangeListener> captor = ArgumentCaptor.forClass(ViewPager.OnPageChangeListener.class);
        when(view.findViewById(R.id.footer_toggle)).thenReturn(footerToggle);
        when(view.findViewById(R.id.player_toggle)).thenReturn(playerToggle);
        when(view.findViewById(R.id.player_track_pager)).thenReturn(trackPager);
        when(view.findViewById(R.id.player_next)).thenReturn(nextButton);
        when(view.findViewById(R.id.player_previous)).thenReturn(previousButton);
        playerPresenter = new PlayerPresenter(view, trackPagerAdapter, listener);
        verify(trackPager).setOnPageChangeListener(captor.capture());
        pagerListener = captor.getValue();
    }


    @Test
    public void constructorSetsTrackPagerAdapterOnTrackPager() {
        verify(trackPager).setAdapter(trackPagerAdapter);
    }

    @Test
    public void constructorSetsPresenterAdListenerOnFooterToggle() {
        verify(footerToggle).setOnClickListener(playerPresenter);
    }

    @Test
    public void constructorSetsPresenterAsListenerOnPlayerToggle() {
        verify(playerToggle).setOnClickListener(playerPresenter);
    }

    @Test
    public void constructorSetsPresenterAsListenerOnNextButton() {
        verify(nextButton).setOnClickListener(playerPresenter);
    }

    @Test
    public void constructorSetsPresenterAsListenerOnPreviousButton() {
        verify(previousButton).setOnClickListener(playerPresenter);
    }

    @Test
    public void onClickWithFooterToggleViewCallsOnTogglePlayOnListener() {
        when(view.getId()).thenReturn(R.id.footer_toggle);
        playerPresenter.onClick(view);
        verify(listener).onTogglePlay();
    }

    @Test
    public void onClickWithPlayerToggleViewCallsOnTogglePlayOnListener() {
        when(view.getId()).thenReturn(R.id.player_toggle);
        playerPresenter.onClick(view);
        verify(listener).onTogglePlay();
    }

    @Test
    public void onClickWithPlayerNextViewCallsOnNextOnListener() {
        when(view.getId()).thenReturn(R.id.player_next);
        playerPresenter.onClick(view);
        verify(listener).onNext();
    }

    @Test
    public void onClickWithPlayerPreviousViewCallsOnPreviousOnListener() {
        when(view.getId()).thenReturn(R.id.player_previous);
        playerPresenter.onClick(view);
        verify(listener).onPrevious();
    }

    @Test
    public void onPageSelectedOnTrackChangeListenerCallsOnTrackChangedOnListener() {
        pagerListener.onPageSelected(3);
        verify(listener).onTrackChanged(3);
    }

    @Test
    public void setQueuePositionCallsSetCurrentItemOnTrackPager() {
        playerPresenter.setQueuePosition(2);
        verify(trackPager).setCurrentItem(2);
    }

    @Test
    public void onPlayQueueChangedNotifiesDataSetChangedOnAdapter() {
        playerPresenter.onPlayQueueChanged();
        verify(trackPagerAdapter).notifyDataSetChanged();
    }

    @Test
    public void onPlayStateChangedWithPlayingStateSetsTogglesToChecked() {
        playerPresenter.onPlayStateChanged(true);
        verify(footerToggle).setChecked(true);
        verify(playerToggle).setChecked(true);
    }

    @Test
    public void onPlayStateChangedWithNotPlayingStateSetsTogglesToNotChecked() {
        playerPresenter.onPlayStateChanged(false);
        verify(footerToggle).setChecked(false);
        verify(playerToggle).setChecked(false);
    }

    @Test
    public void presenterFactoryCreatesPresenterWithTrackPagerFromConstructor() {
        reset(trackPager);
        new PlayerPresenter.Factory(trackPagerAdapter).create(view, listener);
        verify(trackPager).setAdapter(trackPagerAdapter);
    }
}