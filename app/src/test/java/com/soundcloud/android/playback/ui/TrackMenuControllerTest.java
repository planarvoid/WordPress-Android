package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.mockito.Mock;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class TrackMenuControllerTest {

    private TrackMenuController controller;
    private PlayerTrack track;

    @Mock
    private TrackPageListener trackPageListener;

    @Before
    public void setUp() throws Exception {
        track = new PlayerTrack(TestPropertySets.forPlayerTrack());
        controller = new TrackMenuController.Factory().create(Robolectric.application, mock(View.class), trackPageListener);
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
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_REPOSTED.bind(true)));
        controller.setTrack(withoutUser);

        controller.onMenuItemClick(share);

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toEqual(track.getPermalinkUrl());
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("dubstep anthem on SoundCloud");
    }

    @Test
    public void doesNotSendShareIntentForPrivateTracks() {
        MenuItem share = mock(MenuItem.class);
        when(share.getItemId()).thenReturn(R.id.share);
        PlayerTrack privateTrack = new PlayerTrack(PropertySet.from(
                PlayableProperty.IS_PRIVATE.bind(true),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_REPOSTED.bind(true)));
        controller.setTrack(privateTrack);

        controller.onMenuItemClick(share);

        expect(shadowOf(Robolectric.application).getNextStartedActivity()).toBeNull();
    }

    @Test
    public void clickingRepostMenuItemCallsOnRepostWithTrue() throws Exception {
        MenuItem repost = mock(MenuItem.class);
        when(repost.getItemId()).thenReturn(R.id.repost);

        controller.onMenuItemClick(repost);

        verify(trackPageListener).onToggleRepost(true);
    }

    @Test
    public void clickingUnpostMenuItemCallsOnRepostWithFalse() throws Exception {
        MenuItem unpost = mock(MenuItem.class);
        when(unpost.getItemId()).thenReturn(R.id.unpost);

        controller.onMenuItemClick(unpost);

        verify(trackPageListener).onToggleRepost(false);
    }
}