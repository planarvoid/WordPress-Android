package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockObservable;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.collections.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class TrackPageMenuControllerTest {

    private TrackPageMenuController controller;
    private PlayerTrack track;
    private PlayerTrack privateTrack;

    @Mock private Context context;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private RepostOperations repostOperations;
    @Mock private PopupMenuWrapper popupMenuWrapper;
    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;

    private MockObservable<PropertySet> repostObservable;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        track = new PlayerTrack(TestPropertySets.expectedTrackForPlayer());
        privateTrack = new PlayerTrack(TestPropertySets.expectedPrivateTrackForPlayer());

        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        controller = new TrackPageMenuController.Factory(playQueueManager, repostOperations, popupMenuWrapperFactory, eventBus)
                .create(new TextView(new FragmentActivity()));
        controller.setTrack(track);
        repostObservable = TestObservables.emptyObservable();
        when(repostOperations.toggleRepost(eq(track.getUrn()), anyBoolean())).thenReturn(repostObservable);
        when(playQueueManager.getScreenTag()).thenReturn("screen");
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithAllData() {
        MenuItem share = mockMenuItem(R.id.share);

        controller.onMenuItemClick(share, context);

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("dubstep anthem - SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain("Listen to dubstep anthem by squirlex #np on #SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain(track.getPermalinkUrl());
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithoutUser() {
        MenuItem share = mockMenuItem(R.id.share);
        PlayerTrack withoutUser = new PlayerTrack(PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_REPOSTED.bind(true)));
        controller.setTrack(withoutUser);

        controller.onMenuItemClick(share, context);

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("dubstep anthem - SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain("Listen to dubstep anthem #np on #SoundCloud");
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toContain(track.getPermalinkUrl());
    }

    @Test
    public void doesNotSendShareIntentForPrivateTracks() {
        MenuItem share = mockMenuItem(R.id.share);
        controller.setTrack(privateTrack);

        controller.onMenuItemClick(share, context);

        expect(shadowOf(Robolectric.application).getNextStartedActivity()).toBeNull();
    }

    @Test
    public void clickingShareMenuItemEmitsShareEvent() {
        MenuItem share = mockMenuItem(R.id.share);

        controller.onMenuItemClick(share, context);

        UIEvent expectedEvent = UIEvent.fromShare("screen", track.getUrn());
        expectUIEvent(expectedEvent);
    }

    @Test
    public void clickingRepostMenuItemCallsOnRepostWithTrue() {
        MenuItem repost = mockMenuItem(R.id.repost);

        controller.onMenuItemClick(repost, context);

        verify(repostOperations).toggleRepost(track.getUrn(), true);
        expect(repostObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void clickingUnpostMenuItemCallsOnRepostWithFalse() {
        MenuItem unpost = mockMenuItem(R.id.unpost);

        controller.onMenuItemClick(unpost, context);

        verify(repostOperations).toggleRepost(track.getUrn(), false);
        expect(repostObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void clickingRepostMenuItemEmitsRepostEvent() {
        MenuItem repost = mockMenuItem(R.id.repost);
        controller.onMenuItemClick(repost, context);

        UIEvent expectedEvent = UIEvent.fromToggleRepost(true, "screen", track.getUrn());
        expectUIEvent(expectedEvent);
    }

    @Test
    public void clickingUnpostMenuItemEmitsRepostEvent() {
        MenuItem unpost = mockMenuItem(R.id.unpost);
        controller.onMenuItemClick(unpost, context);

        UIEvent expectedEvent = UIEvent.fromToggleRepost(false, "screen", track.getUrn());
        expectUIEvent(expectedEvent);
    }

    @Test
    public void setPublicTrackEnablesShareAndRepostMenuItems() {
        // set track in setup uses public track

        verify(popupMenuWrapper).setItemEnabled(R.id.unpost, true);
        verify(popupMenuWrapper).setItemEnabled(R.id.repost, true);
        verify(popupMenuWrapper).setItemEnabled(R.id.share, true);
    }

    @Test
    public void setPrivateTrackDisablesShareAndRepostMenuItems() {
        controller.setTrack(privateTrack);

        verify(popupMenuWrapper).setItemEnabled(R.id.unpost, false);
        verify(popupMenuWrapper).setItemEnabled(R.id.repost, false);
        verify(popupMenuWrapper).setItemEnabled(R.id.share, false);
    }

    @Test
    public void setProgressSetsCommentTimeInMenu() {
        controller.setProgress(new PlaybackProgress(20000, 40000));

        verify(popupMenuWrapper).setItemText(R.id.comment, "Comment at 0:20");
    }

    @Test
    public void displayScrubPositionSetsCommentTimeInMenu() {
        controller.setProgress(new PlaybackProgress(20000, 40000));

        controller.displayScrubPosition(0.75f);

        verify(popupMenuWrapper).setItemText(R.id.comment, "Comment at 0:30");
    }

    @Test
    public void displayScrubPositionSetsCommentTimeInMenuWithoutPlayback() {
        controller.displayScrubPosition(0.5f);

        verify(popupMenuWrapper).setItemText(R.id.comment, "Comment at 0:10");
    }

    @Test
    public void displayFormattedCommentTimeWhenNoProgressWasSet() {
        verify(popupMenuWrapper).setItemText(R.id.comment, "Comment at 0:00");
    }

    @Test
    public void shouldNotShowTheMenuIfWeHaveAnEmptyTrack() {
        controller.setTrack(PlayerTrack.EMPTY);

        controller.show();

        verify(popupMenuWrapper, never()).show();
    }

    private void expectUIEvent(UIEvent expectedEvent) {
        UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(uiEvent.getKind()).toEqual(expectedEvent.getKind());
        expect(uiEvent.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    private MenuItem mockMenuItem(int menuteItemId) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(menuteItemId);
        return menuItem;
    }
}