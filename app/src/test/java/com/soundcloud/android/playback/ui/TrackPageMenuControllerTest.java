package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class TrackPageMenuControllerTest extends AndroidUnitTest {

    private TrackPageMenuController controller;
    private PlayerTrackState track;
    private PlayerTrackState privateTrack;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private RepostOperations repostOperations;
    @Mock private PopupMenuWrapper popupMenuWrapper;
    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock private TextView textView;

    private Activity activityContext;
    private TestEventBus eventBus = new TestEventBus();
    private PublishSubject<PropertySet> repostSubject = PublishSubject.create();

    @Before
    public void setUp() {
        activityContext = new Activity();
        track = new PlayerTrackState(TestPropertySets.expectedTrackForPlayer(), false, false, null);
        privateTrack = new PlayerTrackState(TestPropertySets.expectedPrivateTrackForPlayer(), false, false, null);

        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(textView.getContext()).thenReturn(new FragmentActivity());
        when(repostOperations.toggleRepost(eq(track.getUrn()), anyBoolean())).thenReturn(repostSubject);
        when(playQueueManager.getScreenTag()).thenReturn("screen");

        controller = new TrackPageMenuController.Factory(playQueueManager, repostOperations, popupMenuWrapperFactory, eventBus)
                .create(textView);
        controller.setTrack(track);
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithAllData() {
        MenuItem share = mockMenuItem(R.id.share);

        controller.onMenuItemClick(share, activityContext);

        Assertions.assertThat(activityContext)
                .nextStartedIntent()
                .containsExtra(Intent.EXTRA_SUBJECT, "dubstep anthem - SoundCloud")
                .containsExtra(Intent.EXTRA_TEXT, "Listen to dubstep anthem by squirlex #np on #SoundCloud\\nhttp://permalink.url");
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithoutUser() {
        MenuItem share = mockMenuItem(R.id.share);
        PlayerTrackState withoutUser = new PlayerTrackState(PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_REPOSTED.bind(true)), false, false, null);
        controller.setTrack(withoutUser);

        controller.onMenuItemClick(share, activityContext);

        Assertions.assertThat(activityContext)
                .nextStartedIntent()
                .containsExtra(Intent.EXTRA_SUBJECT, "dubstep anthem - SoundCloud")
                .containsExtra(Intent.EXTRA_TEXT, "Listen to dubstep anthem #np on #SoundCloud\\nhttp://permalink.url");
    }

    @Test
    public void doesNotSendShareIntentForPrivateTracks() {
        MenuItem share = mockMenuItem(R.id.share);
        controller.setTrack(privateTrack);

        controller.onMenuItemClick(share, activityContext);

        Assertions.assertThat(activityContext)
                .hasNoNextStartedIntent();
    }

    @Test
    public void clickingShareMenuItemEmitsShareEvent() {
        MenuItem share = mockMenuItem(R.id.share);

        controller.onMenuItemClick(share, activityContext);

        UIEvent expectedEvent = UIEvent.fromShare("screen", track.getUrn());
        expectUIEvent(expectedEvent);
    }

    @Test
    public void clickingRepostMenuItemCallsOnRepostWithTrue() {
        MenuItem repost = mockMenuItem(R.id.repost);

        controller.onMenuItemClick(repost, activityContext);

        verify(repostOperations).toggleRepost(track.getUrn(), true);

    }

    @Test
    public void clickingUnpostMenuItemCallsOnRepostWithFalse() {
        MenuItem unpost = mockMenuItem(R.id.unpost);

        controller.onMenuItemClick(unpost, activityContext);

        verify(repostOperations).toggleRepost(track.getUrn(), false);
        assertThat(repostSubject.hasObservers()).isTrue();
    }

    @Test
    public void clickingRepostMenuItemEmitsRepostEvent() {
        MenuItem repost = mockMenuItem(R.id.repost);
        controller.onMenuItemClick(repost, activityContext);

        UIEvent expectedEvent = UIEvent.fromToggleRepost(true, "screen", track.getUrn());
        expectUIEvent(expectedEvent);
    }

    @Test
    public void clickingUnpostMenuItemEmitsRepostEvent() {
        MenuItem unpost = mockMenuItem(R.id.unpost);
        controller.onMenuItemClick(unpost, activityContext);

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
        controller.setTrack(PlayerTrackState.EMPTY);

        controller.show();

        verify(popupMenuWrapper, never()).show();
    }

    private void expectUIEvent(UIEvent expectedEvent) {
        UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(expectedEvent.getKind());
        assertThat(uiEvent.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    private MenuItem mockMenuItem(int menuteItemId) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(menuteItemId);
        return menuItem;
    }
}