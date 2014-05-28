package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.playback.views.ArtworkTrackView;
import com.soundcloud.android.screens.elements.ViewPagerElement;
import com.soundcloud.android.tests.Han;

import android.view.View;
import android.widget.TextView;

public class LegacyPlayerScreen extends Screen {
    private static final Class ACTIVITY = PlayerActivity.class;
    private final ViewPagerElement viewPager;

    public LegacyPlayerScreen(Han solo) {
        super(solo);
        viewPager = new ViewPagerElement(solo, R.id.track_view);
    }

    public void stopPlayback() {
        solo.clickOnView(R.id.pause);
    }

    public PlaylistDetailsScreen goBackToPlaylist() {
        solo.goBack();
        return new PlaylistDetailsScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public View trackTitleElement() {
        return getCurrentPage().findViewById(R.id.playable_title);
    }


    public String trackTitle() {
        return ((TextView)trackTitleElement()).getText().toString();
    }

    private View getCurrentPage() {
        return viewPager.getCurrentPage(ArtworkTrackView.class);
    }
}
