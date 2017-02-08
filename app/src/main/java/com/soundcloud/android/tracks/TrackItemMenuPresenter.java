package com.soundcloud.android.tracks;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.experiments.PlayQueueConfiguration;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.RepostResultSubscriber;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import java.util.Collections;

public class TrackItemMenuPresenter implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final TrackRepository trackRepository;
    private final Context context;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final ShareOperations shareOperations;
    private final PlaylistOperations playlistOperations;
    private final ScreenProvider screenProvider;
    private final StartStationHandler stationHandler;
    private final AccountOperations accountOperations;
    private final PlayQueueConfiguration playQueueConfiguration;
    private final PlayQueueManager playQueueManager;
    private final PlaybackInitiator playbackInitiator;
    private final PlaybackToastHelper playbackToastHelper;
    private final EventTracker eventTracker;

    private FragmentActivity activity;
    private TrackItem track;
    private PromotedSourceInfo promotedSourceInfo;
    private Urn playlistUrn;
    private Urn ownerUrn;
    private Subscription trackSubscription = RxUtils.invalidSubscription();

    @Nullable private RemoveTrackListener removeTrackListener;

    private EventContextMetadata eventContextMetadata;

    public interface RemoveTrackListener {
        RemoveTrackListener EMPTY = new RemoveTrackListener() {
            @Override
            public void onPlaylistTrackRemoved(Urn track) {

            }
        };

        void onPlaylistTrackRemoved(Urn track);
    }

    @Inject
    TrackItemMenuPresenter(PopupMenuWrapper.Factory popupMenuWrapperFactory,
                           TrackRepository trackRepository,
                           EventBus eventBus,
                           Context context,
                           LikeOperations likeOperations,
                           RepostOperations repostOperations,
                           PlaylistOperations playlistOperations,
                           ScreenProvider screenProvider,
                           ShareOperations shareOperations,
                           StartStationHandler stationHandler,
                           AccountOperations accountOperations,
                           PlayQueueConfiguration playQueueConfiguration,
                           PlayQueueManager playQueueManager,
                           PlaybackInitiator playbackInitiator,
                           PlaybackToastHelper playbackToastHelper,
                           EventTracker eventTracker) {
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.trackRepository = trackRepository;
        this.eventBus = eventBus;
        this.context = context;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.playlistOperations = playlistOperations;
        this.screenProvider = screenProvider;
        this.stationHandler = stationHandler;
        this.shareOperations = shareOperations;
        this.accountOperations = accountOperations;
        this.playQueueConfiguration = playQueueConfiguration;
        this.playQueueManager = playQueueManager;
        this.playbackInitiator = playbackInitiator;
        this.playbackToastHelper = playbackToastHelper;
        this.eventTracker = eventTracker;
    }

    public void show(FragmentActivity activity, View button, TrackItem track, int position) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .contextScreen(screenProvider.getLastScreenTag())
                                                                         .pageName(screenProvider.getLastScreenTag())
                                                                         .invokerScreen(ScreenElement.LIST.get());
        show(activity, button, track, builder);
    }

    public void show(FragmentActivity activity,
                     View button,
                     TrackItem track,
                     EventContextMetadata.Builder builder) {
        if (track instanceof PromotedTrackItem) {
            show(activity, button, track, Urn.NOT_SET, Urn.NOT_SET, null,
                 PromotedSourceInfo.fromItem((PromotedTrackItem) track), builder);
        } else {
            show(activity, button, track, Urn.NOT_SET, Urn.NOT_SET, null, null, builder);
        }
    }

    public void show(FragmentActivity activity,
                     View button,
                     TrackItem track,
                     Urn playlistUrn,
                     Urn ownerUrn,
                     RemoveTrackListener removeTrackListener,
                     PromotedSourceInfo promotedSourceInfo,
                     EventContextMetadata.Builder builder) {
        this.activity = activity;
        this.track = track;
        this.removeTrackListener = removeTrackListener;
        this.promotedSourceInfo = promotedSourceInfo;
        this.playlistUrn = playlistUrn;
        this.ownerUrn = ownerUrn;
        this.eventContextMetadata = builder.isFromOverflow(true).build();
        loadTrack(setupMenu(button));
    }

    private PopupMenuWrapper setupMenu(View button) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.track_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
        menu.setItemEnabled(R.id.add_to_likes, false);
        menu.setItemVisible(R.id.add_to_playlist, !isOwnedPlaylist());
        menu.setItemVisible(R.id.remove_from_playlist, isOwnedPlaylist());

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
        menu.setItemVisible(R.id.play_next, playQueueConfiguration.isEnabled());
        menu.setItemEnabled(R.id.play_next, canPlayNext(track));
    }

    private boolean canPlayNext(TrackItem track) {
        return !track.isBlocked();
    }

    private boolean canRepost(TrackItem track) {
        return !accountOperations.isLoggedInUser(track.getCreatorUrn()) && !track.isPrivate();
    }

    private void loadTrack(PopupMenuWrapper menu) {
        trackSubscription.unsubscribe();
        trackSubscription = trackRepository
                .track(track.getUrn())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TrackSubscriber(menu));
    }

    @Override
    public void onDismiss() {
        trackSubscription.unsubscribe();
        trackSubscription = Subscriptions.empty();
        activity = null;
        track = null;
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
                checkState(isOwnedPlaylist());
                final Urn trackUrn = track.getUrn();
                playlistOperations.removeTrackFromPlaylist(playlistUrn, trackUrn)
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(new DefaultSubscriber<Integer>() {
                                      @Override
                                      public void onNext(Integer args) {
                                          if (removeTrackListener != null) {
                                              removeTrackListener.onPlaylistTrackRemoved(trackUrn);
                                          }
                                      }
                                  });
                return true;
            case R.id.start_station:
                handleStation(context);
                return true;
            case R.id.play_next:
                playNext(track.getUrn());
                return true;
            default:
                return false;
        }
    }

    private void handleStation(Context context) {
        stationHandler.openStationWithSeedTrack(context,
                                                track.getUrn(),
                                                UIEvent.fromNavigation(track.getUrn(), eventContextMetadata));
    }

    private void playNext(Urn trackUrn) {
        final String lastScreen = screenProvider.getLastScreenTag();

        if (playQueueManager.isQueueEmpty()) {
            final PlaySessionSource playSessionSource = PlaySessionSource.forPlayNext(lastScreen);
            playbackInitiator.playTracks(Collections.singletonList(trackUrn), 0, playSessionSource)
                             .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
        } else {
            playQueueManager.insertNext(trackUrn);
        }

        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayNext(trackUrn, lastScreen, eventContextMetadata));
    }

    private void handleShare(Context context) {
        shareOperations.share(context, track, eventContextMetadata, getPromotedSource());
    }

    private void showAddToPlaylistDialog() {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(
                track.getUrn(), track.getTitle(), ScreenElement.LIST.get(),
                screenProvider.getLastScreenTag());
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
        final boolean addLike = !track.isLikedByCurrentUser();
        likeOperations.toggleLike(track.getUrn(), addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new LikeToggleSubscriber(context, addLike));

        trackLike(addLike);
    }

    private void handleRepost() {
        final boolean repost = !track.isRepostedByCurrentUser();
        repostOperations.toggleRepost(track.getUrn(), repost)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSubscriber(context));

        trackRepost(repost);
    }

    private static class TrackSubscriber extends DefaultSubscriber<Track> {
        private final PopupMenuWrapper menu;

        TrackSubscriber(PopupMenuWrapper menu) {
            this.menu = menu;
        }

        @Override
        public void onNext(Track track) {
            updateLikeActionTitle(track.userLike());
            updateRepostActionTitle(track.userRepost());
        }

        private void updateLikeActionTitle(boolean isLiked) {
            final MenuItem item = menu.findItem(R.id.add_to_likes);
            if (isLiked) {
                item.setTitle(R.string.btn_unlike);
            } else {
                item.setTitle(R.string.btn_like);
            }
            menu.setItemEnabled(R.id.add_to_likes, true);
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

    private boolean isOwnedPlaylist() {
        return removeTrackListener != null && accountOperations.isLoggedInUser(ownerUrn);
    }

    private PromotedSourceInfo getPromotedSource() {
        return this.promotedSourceInfo;
    }

}
