package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleObserver;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.RepostResultSingleObserver;
import com.soundcloud.android.rx.observers.DefaultMaybeObserver;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import java.util.Collections;

public class TrackItemMenuPresenter implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final TrackItemRepository trackItemRepository;
    private final Context context;
    private final EventBusV2 eventBus;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final SharePresenter sharePresenter;
    private final PlaylistOperations playlistOperations;
    private final ScreenProvider screenProvider;
    private final StartStationHandler stationHandler;
    private final AccountOperations accountOperations;
    private final PlayQueueManager playQueueManager;
    private final PlaybackInitiator playbackInitiator;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final EventTracker eventTracker;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;
    private final FeedbackController feedbackController;
    private final NavigationExecutor navigationExecutor;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    private FragmentActivity activity;
    private TrackItem track;
    private PromotedSourceInfo promotedSourceInfo;
    private Urn playlistUrn;
    private Urn ownerUrn;
    private Disposable trackDisposable = Disposables.disposed();
    private boolean isShowing;

    private EventContextMetadata eventContextMetadata;

    @Inject
    TrackItemMenuPresenter(PopupMenuWrapper.Factory popupMenuWrapperFactory,
                           TrackItemRepository trackItemRepository,
                           EventBusV2 eventBus,
                           Context context,
                           LikeOperations likeOperations,
                           RepostOperations repostOperations,
                           PlaylistOperations playlistOperations,
                           ScreenProvider screenProvider,
                           SharePresenter sharePresenter,
                           StartStationHandler stationHandler,
                           AccountOperations accountOperations,
                           PlayQueueManager playQueueManager,
                           PlaybackInitiator playbackInitiator,
                           PlaybackFeedbackHelper playbackFeedbackHelper,
                           EventTracker eventTracker,
                           ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                           ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper,
                           FeedbackController feedbackController,
                           NavigationExecutor navigationExecutor,
                           PerformanceMetricsEngine performanceMetricsEngine) {
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.trackItemRepository = trackItemRepository;
        this.eventBus = eventBus;
        this.context = context;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.playlistOperations = playlistOperations;
        this.screenProvider = screenProvider;
        this.stationHandler = stationHandler;
        this.sharePresenter = sharePresenter;
        this.accountOperations = accountOperations;
        this.playQueueManager = playQueueManager;
        this.playbackInitiator = playbackInitiator;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.eventTracker = eventTracker;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
        this.feedbackController = feedbackController;
        this.navigationExecutor = navigationExecutor;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public void show(FragmentActivity activity, View button, TrackItem track, int position) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .pageName(screenProvider.getLastScreenTag());
        show(activity, button, track, builder);
    }

    public void show(FragmentActivity activity,
                     View button,
                     TrackItem track,
                     EventContextMetadata.Builder builder) {
        if (track.isPromoted()) {
            show(activity, button, track, Urn.NOT_SET, Urn.NOT_SET, PromotedSourceInfo.fromItem(track), builder);
        } else {
            show(activity, button, track, Urn.NOT_SET, Urn.NOT_SET, null, builder);
        }
    }

    public void show(FragmentActivity activity,
                     View button,
                     TrackItem track,
                     Urn playlistUrn,
                     Urn ownerUrn,
                     PromotedSourceInfo promotedSourceInfo,
                     EventContextMetadata.Builder builder) {
        if (!isShowing) {
            this.activity = activity;
            this.track = track;
            this.promotedSourceInfo = promotedSourceInfo;
            this.playlistUrn = playlistUrn;
            this.ownerUrn = ownerUrn;
            this.eventContextMetadata = builder.isFromOverflow(true).build();
            loadTrack(setupMenu(button, ownerUrn));
            this.isShowing = true;
        }
    }

    private PopupMenuWrapper setupMenu(View button, Urn ownerUrn) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.track_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
        menu.setItemEnabled(R.id.add_to_likes, false);
        menu.setItemVisible(R.id.add_to_playlist, !isOwnedPlaylist(ownerUrn));
        menu.setItemVisible(R.id.remove_from_playlist, isOwnedPlaylist(ownerUrn));

        configureStationOptions(button.getContext(), menu);
        configureAdditionalEngagementsOptions(menu);
        configurePlayNext(menu);

        menu.show();
        return menu;
    }

    private void configureStationOptions(Context context, PopupMenuWrapper menu) {
        menu.findItem(R.id.start_station).setTitle(context.getText(R.string.stations_open_station));
        menu.setItemEnabled(R.id.start_station, IOUtils.isConnected(context));
    }

    private void configureAdditionalEngagementsOptions(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.toggle_repost, canRepost(track));
        menu.setItemVisible(R.id.share, !track.isPrivate());
    }

    private void configurePlayNext(PopupMenuWrapper menu) {
        menu.setItemVisible(R.id.play_next, true);
        menu.setItemEnabled(R.id.play_next, canPlayNext(track));
    }

    private boolean canPlayNext(TrackItem trackItem) {
        return !trackItem.isBlocked();
    }

    private boolean canRepost(TrackItem track) {
        return !accountOperations.isLoggedInUser(track.creatorUrn()) && !track.isPrivate();
    }

    private void loadTrack(PopupMenuWrapper menu) {
        trackDisposable.dispose();
        trackDisposable = trackItemRepository
                .track(track.getUrn())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new TrackSubscriber(menu, changeLikeToSaveExperimentStringHelper));
    }

    @Override
    public void onDismiss() {
        trackDisposable.dispose();
        trackDisposable = Disposables.empty();
        activity = null;
        track = null;
        isShowing = false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.add_to_likes:
                handleLike();
                return true;
            case R.id.share:
                handleShare(context);
                return true;
            case R.id.toggle_repost:
                handleRepost();
                return true;
            case R.id.add_to_playlist:
                showAddToPlaylistDialog();
                return true;
            case R.id.remove_from_playlist:
                if (!isOwnedPlaylist(this.ownerUrn)) {
                    throw new IllegalStateException("Cannot remove from someone else's playlist");
                }
                final Urn trackUrn = track.getUrn();
                playlistOperations.removeTrackFromPlaylist(playlistUrn, trackUrn)
                                  .observeOn(rx.android.schedulers.AndroidSchedulers.mainThread())
                                  .subscribe(new DefaultSubscriber<>());
                return true;
            case R.id.start_station:
                handleStation();
                return true;
            case R.id.play_next:
                playNext(track.getUrn());
                return true;
            default:
                return false;
        }
    }

    private void handleStation() {
        stationHandler.openStationWithSeedTrack(
                track.getUrn(),
                                                UIEvent.fromNavigation(track.getUrn(), eventContextMetadata));
    }

    private void playNext(Urn trackUrn) {
        final String lastScreen = screenProvider.getLastScreenTag();

        if (playQueueManager.isQueueEmpty()) {
            final PlaySessionSource playSessionSource = PlaySessionSource.forPlayNext(lastScreen);
            playbackInitiator.playTracks(Collections.singletonList(trackUrn), 0, playSessionSource)
                  .subscribe(new ExpandPlayerSingleObserver(eventBus, playbackFeedbackHelper, performanceMetricsEngine));
        } else {
            playQueueManager.insertNext(trackUrn);
        }

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayNext(trackUrn, lastScreen, eventContextMetadata));
    }

    private void handleShare(Context context) {
        sharePresenter.share(context, track, eventContextMetadata, getPromotedSource());
    }

    private void showAddToPlaylistDialog() {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track.getUrn(), track.title());
        from.show(activity.getFragmentManager());
    }

    private void trackLike(boolean addLike) {
        eventTracker.trackEngagement(UIEvent.fromToggleLike(addLike,
                                                            track.getUrn(),
                                                            eventContextMetadata,
                                                            getPromotedSource(),
                                                            EntityMetadata.from(track)));
    }

    private void trackRepost(boolean repost) {
        eventTracker.trackEngagement(UIEvent.fromToggleRepost(repost,
                                                              track.getUrn(),
                                                              eventContextMetadata,
                                                              getPromotedSource(),
                                                              EntityMetadata.from(track)));

    }

    private void handleLike() {
        final boolean addLike = !track.isUserLike();
        likeOperations.toggleLike(track.getUrn(), addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new LikeToggleObserver(context, addLike, changeLikeToSaveExperiment, feedbackController, navigationExecutor));

        trackLike(addLike);
    }

    private void handleRepost() {
        final boolean repost = !track.isUserRepost();
        repostOperations.toggleRepost(track.getUrn(), repost)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSingleObserver(context));

        trackRepost(repost);
    }

    private static class TrackSubscriber extends DefaultMaybeObserver<TrackItem> {
        private final PopupMenuWrapper menu;
        private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

        TrackSubscriber(PopupMenuWrapper menu, ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
            this.menu = menu;
            this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
        }

        @Override
        public void onSuccess(TrackItem track) {
            updateLikeActionTitle(track.isUserLike());
            updateRepostActionTitle(track.isUserRepost());
        }

        private void updateLikeActionTitle(boolean isLiked) {
            final String addToLikesString = isLiked
                                            ? changeLikeToSaveExperimentStringHelper.getString(ExperimentString.UNLIKE)
                                            : changeLikeToSaveExperimentStringHelper.getString(ExperimentString.LIKE);
            menu.setItemText(R.id.add_to_likes, addToLikesString);
            menu.setItemEnabled(R.id.add_to_likes, true);
            menu.setItemVisible(R.id.add_to_likes, true);
        }

        private void updateRepostActionTitle(boolean isReposted) {
            final MenuItem item = menu.findItem(R.id.toggle_repost);
            if (isReposted) {
                item.setTitle(R.string.unpost);
            } else {
                item.setTitle(R.string.repost);
            }
        }
    }

    private boolean isOwnedPlaylist(Urn ownerUrn) {
        return ownerUrn != null && !ownerUrn.equals(Urn.NOT_SET) & accountOperations.isLoggedInUser(ownerUrn);
    }

    private PromotedSourceInfo getPromotedSource() {
        return this.promotedSourceInfo;
    }

}
