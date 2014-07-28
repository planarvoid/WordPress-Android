package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class TrackMenuControllerTest {

    private TrackMenuController controller;
    private PlayerTrack track;

    @Before
    public void setUp() throws Exception {
        track = new PlayerTrack(TestPropertySets.forPlayerTrack());
        controller = new TrackMenuController(Robolectric.application, mock(View.class));
        controller.setTrack(track);
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithAllData() {
        MenuItem share = mock(MenuItem.class);
        when(share.getItemId()).thenReturn(R.id.share);

        controller.onMenuItemClick(share);

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toEqual(track.getPermalinkUrl());
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("dubstep anthem by squirlex on SoundCloud");
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithoutUser() {
        MenuItem share = mock(MenuItem.class);
        when(share.getItemId()).thenReturn(R.id.share);
        PlayerTrack withoutUser = new PlayerTrack(PropertySet.from(
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url")));
        controller.setTrack(withoutUser);

        controller.onMenuItemClick(share);

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toEqual(track.getPermalinkUrl());
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("dubstep anthem on SoundCloud");
    }

}