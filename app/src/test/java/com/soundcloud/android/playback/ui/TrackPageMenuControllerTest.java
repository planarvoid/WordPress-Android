package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StartStationPresenter;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class TrackPageMenuControllerTest extends AndroidUnitTest {

    private TrackPageMenuController controller;
    private PlayerTrackState track;
    private PlayerTrackState privateTrack;

    @Mock private FeatureFlags featureFlags;
    @Mock private StartStationPresenter startStationPresenter;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private RepostOperations repostOperations;
    @Mock private PopupMenuWrapper popupMenuWrapper;
    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock private TextView textView;
    @Mock private ShareOperations shareOperations;

    private Activity activityContext;
    private TestEventBus eventBus = new TestEventBus();
    private PublishSubject<PropertySet> repostSubject = PublishSubject.create();
    private PropertySet sourceTrack;

    @Before
    public void setUp() {
        activityContext = new Activity();
        sourceTrack = TestPropertySets.expectedTrackForPlayer();
        track = new PlayerTrackState(sourceTrack, false, false, null);
        privateTrack = new PlayerTrackState(TestPropertySets.expectedPrivateTrackForPlayer(), false, false, null);

        when(popupMenuWrapperFactory.build(any(Context.class), any(View.class))).thenReturn(popupMenuWrapper);
        when(textView.getContext()).thenReturn(new FragmentActivity());
        when(repostOperations.toggleRepost(eq(track.getUrn()), anyBoolean())).thenReturn(repostSubject);
        when(playQueueManager.getScreenTag()).thenReturn("screen");

        controller = new TrackPageMenuController.Factory(featureFlags, playQueueManager, repostOperations, popupMenuWrapperFactory, startStationPresenter, eventBus, shareOperations)
                .create(textView);
        controller.setTrack(track);
    }

    @Test
    public void clickingStartStationStartsStationForTrack() {
        MenuItem stationItem = mockMenuItem(R.id.start_station);

        controller.onMenuItemClick(stationItem, activityContext);

        verify(startStationPresenter).startStationForTrack(activityContext, track.getUrn());
    }

    @Test
    public void clickingStartStationOnBlockedTrackStartsStationWithoutPrependingSeed() {
        sourceTrack.put(TrackProperty.BLOCKED, true);
        MenuItem stationItem = mockMenuItem(R.id.start_station);

        controller.onMenuItemClick(stationItem, activityContext);

        verify(startStationPresenter).startStation(activityContext, Urn.forTrackStation(track.getUrn().getNumericId()));
    }

    @Test
    public void clickingShareMenuItemSendsShareIntentWithAllData() {
        MenuItem share = mockMenuItem(R.id.share);

        controller.onMenuItemClick(share, activityContext);

        EventContextMetadata eventContextMetadata = EventContextMetadata.builder()
                .contextScreen("screen")
                .pageName(Screen.PLAYER_MAIN.get())
                .pageUrn(track.getUrn())
                .isFromOverflow(true)
                .build();
        verify(shareOperations).share(activityContext, track.getSource(), eventContextMetadata, null);
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

        UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_REPOST);
    }

    @Test
    public void clickingUnpostMenuItemEmitsRepostEvent() {
        MenuItem unpost = mockMenuItem(R.id.unpost);
        controller.onMenuItemClick(unpost, activityContext);

        UIEvent uiEvent = (UIEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(uiEvent.getKind()).isEqualTo(UIEvent.KIND_UNREPOST);
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
    public void clearProgressSetsCommentTimeToZeroInMenu() {
        controller.setProgress(new PlaybackProgress(20000, 40000));
        controller.clearProgress();

        verify(popupMenuWrapper, times(2)).setItemText(R.id.comment, "Comment at 0:00");
    }

    @Test
    public void displayScrubPositionSetsCommentTimeInMenu() {
        controller.setProgress(new PlaybackProgress(20000, 40000));

        controller.displayScrubPosition(0.75f, 1.1f);

        verify(popupMenuWrapper).setItemText(R.id.comment, "Comment at 0:20");
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

    @Test
    public void shouldHideCommentOptionWhenTrackIsNotCommentable() {
        verify(popupMenuWrapper).setItemVisible(R.id.comment, true);

        final PropertySet notCommentable = TestPropertySets.expectedTrackForPlayer().put(TrackProperty.IS_COMMENTABLE, false);
        track = new PlayerTrackState(notCommentable, false, false, null);
        controller.setTrack(track);

        verify(popupMenuWrapper).setItemVisible(R.id.comment, false);
    }

    private MenuItem mockMenuItem(int menuteItemId) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(menuteItemId);
        return menuItem;
    }
}
