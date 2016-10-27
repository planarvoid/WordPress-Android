package com.soundcloud.android.discovery.charts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

public class AllGenresPresenterTest extends AndroidUnitTest {
    @Mock private ChartsTracker chartsTracker;
    @Mock private Bundle bundle;

    private AllGenresPresenter presenter;

    @Before
    public void setUp() {
        presenter = new AllGenresPresenter(resources(), chartsTracker);
    }

    @Test
    public void shouldReturnMusicScreenForDefaultTabShowing() {
        AppCompatActivity activity = createActivity();
        presenter.onCreate(activity, bundle);

        final Screen screen = presenter.getScreen();
        assertThat(screen).isEqualTo(Screen.MUSIC_GENRES);
    }

    @Test
    public void shouldTrackEventForSwitchingToAudioTab() {
        AppCompatActivity activity = createActivity();
        final ViewPager pager = (ViewPager) activity.findViewById(R.id.pager);
        presenter.onCreate(activity, bundle);
        reset(chartsTracker);

        pager.setCurrentItem(1);

        final Screen screen = presenter.getScreen();
        assertThat(screen).isEqualTo(Screen.AUDIO_GENRES);
        verify(chartsTracker).genrePageSelected(Screen.AUDIO_GENRES);
    }

    @Test
    public void shouldTrackEventForSwitchingToMusicTab() {
        AppCompatActivity activity = createActivity();
        final ViewPager pager = (ViewPager) activity.findViewById(R.id.pager);
        presenter.onCreate(activity, bundle);
        pager.setCurrentItem(1);
        reset(chartsTracker);

        pager.setCurrentItem(0);

        final Screen screen = presenter.getScreen();
        assertThat(screen).isEqualTo(Screen.MUSIC_GENRES);
        verify(chartsTracker).genrePageSelected(Screen.MUSIC_GENRES);
    }

    @NonNull
    private AppCompatActivity createActivity() {
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().get();
        final LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View inflated = layoutInflater.inflate(R.layout.tabbed_activity_content, new FrameLayout(context()), false);
        activity.setContentView(inflated);
        return activity;
    }
}
