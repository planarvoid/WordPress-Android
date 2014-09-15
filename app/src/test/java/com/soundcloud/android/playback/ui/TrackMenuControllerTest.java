package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockObservable;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;
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
public class TrackMenuControllerTest {

    private TrackMenuController controller;
    private PlayerTrack track;
    private PlayerTrack privateTrack;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private SoundAssociationOperations soundAssociationOps;
    @Mock private PopupMenuWrapper popupMenuWrapper;
    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;

    private MockObservable<PropertySet> repostObservable;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        track = new PlayerTrack(TestPropertySets.expectedTrackForPlayer());
        privateTrack = new PlayerTrack(TestPropertySets.expectedPrivateTrackForPlayer());

        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        controller = new TrackMenuController.Factory(playQueueManager, soundAssociationOps, popupMenuWrapperFactory, eventBus)
                .create(new TextView(new FragmentActivity()));
        controller.setTrack(track);
        repostObservable = TestObservables.emptyObservable();
        when(soundAssociationOps.toggleRepost(eq(track.getUrn()), anyBoolean())).thenReturn(repostObservable);
        when(playQueueManager.getScreenTag()).thenReturn("screen");
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithAllData() {
        MenuItem share = mockMenuItem(R.id.share);

        controller.onMenuItemClick(share);

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toEqual(track.getPermalinkUrl());
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("Listen to dubstep anthem by squirlex #np on #SoundCloud");
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

        controller.onMenuItemClick(share);

        Intent shareIntent = shadowOf(Robolectric.application).getNextStartedActivity();
        expect(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).toEqual(track.getPermalinkUrl());
        expect(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).toEqual("Listen to dubstep anthem #np on #SoundCloud");
    }

    @Test
    public void doesNotSendShareIntentForPrivateTracks() {
        MenuItem share = mockMenuItem(R.id.share);
        controller.setTrack(privateTrack);

        controller.onMenuItemClick(share);

        expect(shadowOf(Robolectric.application).getNextStartedActivity()).toBeNull();
    }

    @Test
    public void clickingShareMenuItemEmitsShareEvent() {
        MenuItem share = mockMenuItem(R.id.share);

        controller.onMenuItemClick(share);

        UIEvent expectedEvent = UIEvent.fromShare("screen", track.getUrn());
        expectUIEvent(expectedEvent);
    }

    @Test
    public void clickingRepostMenuItemCallsOnRepostWithTrue() {
        MenuItem repost = mockMenuItem(R.id.repost);

        controller.onMenuItemClick(repost);

        verify(soundAssociationOps).toggleRepost(track.getUrn(), true);
        expect(repostObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void clickingUnpostMenuItemCallsOnRepostWithFalse() {
        MenuItem unpost = mockMenuItem(R.id.unpost);

        controller.onMenuItemClick(unpost);

        verify(soundAssociationOps).toggleRepost(track.getUrn(), false);
        expect(repostObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void clickingRepostMenuItemEmitsRepostEvent() {
        MenuItem repost = mockMenuItem(R.id.repost);
        controller.onMenuItemClick(repost);

        UIEvent expectedEvent = UIEvent.fromToggleRepost(true, "screen", track.getUrn());
        expectUIEvent(expectedEvent);
    }

    @Test
    public void clickingUnpostMenuItemEmitsRepostEvent() {
        MenuItem unpost = mockMenuItem(R.id.unpost);
        controller.onMenuItemClick(unpost);

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

    private void expectUIEvent(UIEvent expectedEvent) {
        UIEvent uiEvent = eventBus.lastEventOn(EventQueue.UI);
        expect(uiEvent.getKind()).toEqual(expectedEvent.getKind());
        expect(uiEvent.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    private MenuItem mockMenuItem(int menuteItemId) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(menuteItemId);
        return menuItem;
    }
}