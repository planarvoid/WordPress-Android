package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.comments.AddCommentDialogFragment;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.rx.observers.LambdaMaybeObserver;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.tracks.TrackInfoFragment;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class TrackPageMenuController
        implements ProgressAware, ScrubController.OnScrubListener, PopupMenuWrapper.PopupMenuWrapperListener {

    private static final String INFO_DIALOG_TAG = "info_dialog";
    private static final String ADD_COMMENT_DIALOG_TAG = "add_comment_dialog";

    private final FragmentActivity activity;
    private final PopupMenuWrapper popupMenuWrapper;
    private final PlayQueueManager playQueueManager;
    private final RepostOperations repostOperations;
    private final AccountOperations accountOperations;
    private final EventBusV2 eventBus;
    private final SharePresenter sharePresenter;
    private final String commentAtUnformatted;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final Navigator navigator;
    private final EventTracker eventTracker;

    private PlayerTrackState track = PlayerTrackState.EMPTY;
    private PlaybackProgress lastProgress = PlaybackProgress.empty();

    private long commentPosition;

    private TrackPageMenuController(PlayQueueManager playQueueManager,
                                    RepostOperations repostOperations,
                                    FragmentActivity context,
                                    PopupMenuWrapper popupMenuWrapper,
                                    AccountOperations accountOperations,
                                    EventBusV2 eventBus,
                                    SharePresenter sharePresenter,
                                    PerformanceMetricsEngine performanceMetricsEngine,
                                    Navigator navigator,
                                    EventTracker eventTracker) {
        this.playQueueManager = playQueueManager;
        this.repostOperations = repostOperations;
        this.activity = context;
        this.popupMenuWrapper = popupMenuWrapper;
        this.accountOperations = accountOperations;
        this.eventBus = eventBus;
        this.sharePresenter = sharePresenter;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.eventTracker = eventTracker;
        this.commentAtUnformatted = activity.getString(R.string.comment_at_time);
        this.navigator = navigator;
        setupMenu();
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        lastProgress = progress;
        updateCommentPosition(lastProgress.getPosition());
    }

    @Override
    public void clearProgress() {
        setProgress(PlaybackProgress.empty());
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        // We only care about position
    }

    @Override
    public void displayScrubPosition(float actualPosition, float boundedPosition) {
        updateCommentPosition((long) (boundedPosition * track.getFullDuration()));
    }

    private void updateCommentPosition(long position) {
        commentPosition = position;
        if (track.isCommentable()) {
            final String timestamp = ScTextUtils.formatTimestamp(position, TimeUnit.MILLISECONDS);
            setCommentMenuItemText(String.format(commentAtUnformatted, timestamp));
        }
    }

    private void setCommentMenuItemText(CharSequence text) {
        popupMenuWrapper.setItemText(R.id.comment, text);
    }

    private void setupMenu() {
        popupMenuWrapper.inflate(R.menu.player_page_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);
    }

    private void initStationsOption() {
        popupMenuWrapper.findItem(R.id.start_station).setTitle(activity.getText(R.string.stations_open_station));
        popupMenuWrapper.setItemEnabled(R.id.start_station, IOUtils.isConnected(activity));
    }

    private void initCommentOption() {
        setCommentMenuItemText(popupMenuWrapper.findItem(R.id.comment).getTitle());
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.share:
                handleShare(context);
                return true;
            case R.id.repost:
                handleRepostToggle(true, track.getUrn());
                return true;
            case R.id.unpost:
                handleRepostToggle(false, track.getUrn());
                return true;
            case R.id.info:
                TrackInfoFragment.create(track.getUrn()).show(activity.getSupportFragmentManager(), INFO_DIALOG_TAG);
                return true;
            case R.id.comment:
                handleComment();
                return true;
            case R.id.add_to_playlist:
                showAddToPlaylistDialog(track);
                return true;
            case R.id.start_station:
                handleStartStation();
                return true;
            case R.id.go_to_artist:
                handleGoToArtist();
                return true;
            default:
                return false;
        }
    }

    void handleGoToArtist() {
        final Urn userUrn = track.getUserUrn();
        eventTracker.trackClick(UIEvent.fromItemNavigation(userUrn, EventContextMetadata.builder().pageName(Screen.PLAYER_MAIN.get()).build()));

        eventBus.queue(EventQueue.PLAYER_UI)
                .filter(PlayerUIEvent.PLAYER_IS_COLLAPSED_V2)
                .firstElement()
                .subscribe(LambdaMaybeObserver.onNext(__ -> navigator.navigateTo(NavigationTarget.forProfile(userUrn))));

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());
    }

    private void handleStartStation() {
        eventBus.queue(EventQueue.PLAYER_UI)
                .filter(PlayerUIEvent.PLAYER_IS_COLLAPSED_V2)
                .firstElement()
                .subscribe(new StartStationPageObserver(track));
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayerAutomatically());
    }

    void handleShare(Context context) {
        Urn trackUrn = track.getUrn();
        if (track.getSource().isPresent()) {
            sharePresenter.share(context,
                                 track.getSource().get(),
                                 getContextMetadata(trackUrn),
                                 playQueueManager.getCurrentPromotedSourceInfo(trackUrn));
        }
    }

    private void handleComment() {
        track.getSource().ifPresent(source -> {
            final AddCommentDialogFragment fragment = AddCommentDialogFragment.create(source, commentPosition, playQueueManager.getScreenTag());
            fragment.show(activity.getFragmentManager(), ADD_COMMENT_DIALOG_TAG);
        });
    }

    private void handleRepostToggle(boolean wasReposted, Urn trackUrn) {
        repostOperations.toggleRepost(trackUrn, wasReposted).subscribe(new DefaultSingleObserver<>());

        eventBus.publish(EventQueue.TRACKING,
                         UIEvent.fromToggleRepost(wasReposted,
                                                  trackUrn,
                                                  getContextMetadata(trackUrn),
                                                  playQueueManager.getCurrentPromotedSourceInfo(trackUrn),
                                                  EntityMetadata.from(track)));
    }

    private EventContextMetadata getContextMetadata(Urn trackUrn) {
        return EventContextMetadata.builder()
                                   .pageName(Screen.PLAYER_MAIN.get())
                                   .pageUrn(trackUrn)
                                   .trackSourceInfo(playQueueManager.getCurrentTrackSourceInfo())
                                   .isFromOverflow(true)
                                   .build();
    }

    private void showAddToPlaylistDialog(PlayerTrackState track) {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track.getUrn(), track.getTitle());
        from.show(activity.getFragmentManager());
    }

    public void setTrack(PlayerTrackState track) {
        this.track = track;
        setRepostVisibility(track);
        setShareVisibility(track.isPrivate());
        setCommentsVisibility(track.isCommentable());
    }

    private void setRepostVisibility(PlayerTrackState track) {
        if (canRepost(track)) {
            setUserRepostVisibility(track.isUserRepost());
        } else {
            popupMenuWrapper.setItemVisible(R.id.unpost, false);
            popupMenuWrapper.setItemVisible(R.id.repost, false);
        }
    }

    private boolean canRepost(PlayerTrackState track) {
        return !accountOperations.isLoggedInUser(track.getUserUrn()) && !track.isPrivate();
    }

    private void setCommentsVisibility(boolean isCommentable) {
        popupMenuWrapper.setItemVisible(R.id.comment, isCommentable);
        updateCommentPosition(lastProgress.getPosition());
    }

    void setIsUserRepost(boolean isUserRepost) {
        if (canRepost(track)) {
            setUserRepostVisibility(isUserRepost);
        }
    }

    private void setUserRepostVisibility(boolean isUserRepost) {
        popupMenuWrapper.setItemVisible(R.id.unpost, isUserRepost);
        popupMenuWrapper.setItemVisible(R.id.repost, !isUserRepost);
    }

    private void setShareVisibility(boolean isPrivate) {
        popupMenuWrapper.setItemEnabled(R.id.share, !isPrivate);
    }

    public void show() {
        if (track != PlayerTrackState.EMPTY) {
            initStationsOption();
            initCommentOption();
            popupMenuWrapper.show();
        }
    }

    public void dismiss() {
        popupMenuWrapper.dismiss();
    }

    @Override
    public void onDismiss() {
        // no-op
    }

    static class Factory {
        private final PlayQueueManager playQueueManager;
        private final RepostOperations repostOperations;
        private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
        private final AccountOperations accountOperations;
        private final EventBusV2 eventBus;
        private final SharePresenter sharePresenter;
        private final PerformanceMetricsEngine performanceMetricsEngine;
        private final Navigator navigator;
        private final EventTracker eventTracker;

        @Inject
        Factory(PlayQueueManager playQueueManager,
                RepostOperations repostOperations,
                PopupMenuWrapper.Factory popupMenuWrapperFactory,
                AccountOperations accountOperations,
                EventBusV2 eventBus,
                SharePresenter sharePresenter,
                PerformanceMetricsEngine performanceMetricsEngine,
                Navigator navigator,
                EventTracker eventTracker) {
            this.playQueueManager = playQueueManager;
            this.repostOperations = repostOperations;
            this.popupMenuWrapperFactory = popupMenuWrapperFactory;
            this.accountOperations = accountOperations;
            this.eventBus = eventBus;
            this.sharePresenter = sharePresenter;
            this.performanceMetricsEngine = performanceMetricsEngine;
            this.navigator = navigator;
            this.eventTracker = eventTracker;
        }

        TrackPageMenuController create(View anchorView) {
            return new TrackPageMenuController(
                    playQueueManager,
                    repostOperations,
                    getFragmentActivity(anchorView),
                    popupMenuWrapperFactory.build(getFragmentActivity(anchorView), anchorView),
                    accountOperations,
                    eventBus,
                    sharePresenter,
                    performanceMetricsEngine,
                    navigator,
                    eventTracker);
        }
    }

    private class StartStationPageObserver extends DefaultMaybeObserver<PlayerUIEvent> {
        private final PlayerTrackState track;

        StartStationPageObserver(PlayerTrackState track) {
            this.track = track;
        }

        @Override
        public void onSuccess(PlayerUIEvent unused) {
            super.onSuccess(unused);
            final Urn stationUrn = Urn.forTrackStation(track.getUrn().getNumericId());

            performanceMetricsEngine.startMeasuring(MetricType.LOAD_STATION);

            final NavigationTarget navigationTarget;
            if (track.isBlocked()) {
                navigationTarget = NavigationTarget.forStationInfo(stationUrn, Optional.absent(), Optional.of(STATIONS), Optional.absent());
            } else {
                navigationTarget = NavigationTarget.forStationInfo(stationUrn, Optional.of(track.getUrn()), Optional.of(STATIONS), Optional.absent());
            }
            navigator.navigateTo(navigationTarget);
        }
    }
}
