package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity;
import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistFragment;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public class SystemPlaylistScreen extends Screen {
    private static final Class ACTIVITY = SystemPlaylistActivity.class;

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public SystemPlaylistScreen(Han solo) {
        super(solo);
        waitForFragment();
    }

    @Override
    public boolean isVisible() {
        return waitForFragment();
    }

    private boolean waitForFragment() {
        return waiter.waitForFragmentByTag(SystemPlaylistFragment.TAG);
    }

    public VisualPlayerElement clickHeaderPlay() {
        headerPlayButton().click();
        waiter.waitForPlaybackToBePlaying();
        return new VisualPlayerElement(testDriver);
    }

    public TextElement title() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.system_playlist_title)));

    }

    public TrackItemElement toggleTrackLike(int index) {
        final TrackItemElement track = getTrack(index);

        track.clickOverflowButton().toggleLike();

        return track;
    }

    private TrackItemElement getTrack(int index) {
        final TrackItemElement trackItemElement = getTracks().get(index);

        testDriver.scrollDownOneQuarter();

        return trackItemElement;
    }


    public List<TrackItemElement> getTracks() {
        scrollToItem(With.id(com.soundcloud.android.R.id.track_list_item));
        return Lists.transform(
                testDriver.findOnScreenElements(With.id(com.soundcloud.android.R.id.track_list_item)),
                toTrackItemElement
        );
    }

    private ViewElement headerPlayButton() {
        return testDriver.findOnScreenElement(With.id(R.id.btn_play));
    }

    private final Function<ViewElement, TrackItemElement> toTrackItemElement = viewElement -> new TrackItemElement(testDriver, viewElement);
}
