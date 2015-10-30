package com.soundcloud.android.tracks;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.playlists.RepostResultSubscriber;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.stations.StartStationPresenter;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

public final class TrackItemMenuPresenter implements PopupMenuWrapper.PopupMenuWrapperListener {

    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
    private final TrackRepository trackRepository;
    private final Context context;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final ShareOperations shareOperations;
    private final PlaylistOperations playlistOperations;
    private final ScreenProvider screenProvider;
    private final PlaybackInitiator playbackInitiator;
    private final PlaybackToastHelper playbackToastHelper;
    private final FeatureFlags featureFlags;
    private final DelayedLoadingDialogPresenter.Builder dialogBuilder;
    private final StartStationPresenter startStationPresenter;
    private final AccountOperations accountOperations;

    private FragmentActivity activity;
    private TrackItem track;
    private PromotedSourceInfo promotedSourceInfo;
    private OverflowMenuOptions menuOptions;
    private Urn pageUrn;
    private int positionInAdapter;
    private Subscription trackSubscription = RxUtils.invalidSubscription();
    private Subscription relatedTracksPlaybackSubscription = RxUtils.invalidSubscription();

    @Nullable private RemoveTrackListener removeTrackListener;

    public interface RemoveTrackListener {
        void onPlaylistTrackRemoved(int position);

        Urn getPlaylistUrn();
    }

    @Inject
    TrackItemMenuPresenter(PopupMenuWrapper.Factory popupMenuWrapperFactory,
                           TrackRepository trackRepository,
                           EventBus eventBus, Context context,
                           LikeOperations likeOperations,
                           RepostOperations repostOperations, PlaylistOperations playlistOperations,
                           ScreenProvider screenProvider,
                           PlaybackInitiator playbackInitiator,
                           PlaybackToastHelper playbackToastHelper,
                           FeatureFlags featureFlags,
                           ShareOperations shareOperations,
                           DelayedLoadingDialogPresenter.Builder dialogBuilder,
                           StartStationPresenter startStationPresenter, AccountOperations accountOperations) {
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        this.trackRepository = trackRepository;
        this.eventBus = eventBus;
        this.context = context;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.playlistOperations = playlistOperations;
        this.screenProvider = screenProvider;
        this.playbackInitiator = playbackInitiator;
        this.playbackToastHelper = playbackToastHelper;
        this.featureFlags = featureFlags;
        this.dialogBuilder = dialogBuilder;
        this.startStationPresenter = startStationPresenter;
        this.shareOperations = shareOperations;
        this.accountOperations = accountOperations;
    }

    public void show(FragmentActivity activity, View button, TrackItem track, int position, OverflowMenuOptions options) {
        if (track instanceof PromotedTrackItem) {
            show(activity, button, track, position, Urn.NOT_SET, null,
                    PromotedSourceInfo.fromItem((PromotedTrackItem) track), options);
        } else {
            show(activity, button, track, position, Urn.NOT_SET, null, null, options);
        }
    }

    public void show(FragmentActivity activity, View button, TrackItem track, int position) {
        show(activity, button, track, position, OverflowMenuOptions.builder().build());
    }

    public void show(FragmentActivity activity, View button, TrackItem track, int positionInAdapter, Urn pageUrn,
                     RemoveTrackListener removeTrackListener, PromotedSourceInfo promotedSourceInfo,
                     OverflowMenuOptions menuOptions) {
        this.activity = activity;
        this.track = track;
        this.positionInAdapter = positionInAdapter;
        this.removeTrackListener = removeTrackListener;
        this.promotedSourceInfo = promotedSourceInfo;
        this.pageUrn = pageUrn;
        this.menuOptions = menuOptions;
        final PopupMenuWrapper menu = setupMenu(button);
        loadTrack(menu);
    }

    private PopupMenuWrapper setupMenu(View button) {
        PopupMenuWrapper menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.track_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
        menu.setItemEnabled(R.id.add_to_likes, false);
        menu.setItemVisible(R.id.add_to_playlist, !isOwnedPlaylist());
        menu.setItemVisible(R.id.remove_from_playlist, isOwnedPlaylist());
        menu.setItemVisible(R.id.play_related_tracks, featureFlags.isEnabled(Flag.PLAY_RELATED_TRACKS));
        menu.setItemEnabled(R.id.play_related_tracks, IOUtils.isConnected(button.getContext()));
        menu.setItemVisible(R.id.start_station, featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH));
        menu.setItemEnabled(R.id.start_station, IOUtils.isConnected(button.getContext()));

        configureAdditionalEngagementsOptions(menu);
        menu.show();
        return menu;
    }

    private void configureAdditionalEngagementsOptions(PopupMenuWrapper menu) {
        if (featureFlags.isEnabled(Flag.NEW_STREAM) && menuOptions.showAllEngagements()) {
            menu.setItemVisible(R.id.toggle_repost, canRepost(track));
            menu.setItemVisible(R.id.share, !track.isPrivate());
        }
    }

    private boolean canRepost(TrackItem track) {
        return !accountOperations.isLoggedInUser(track.getCreatorUrn()) && !track.isPrivate();
    }

    private void loadTrack(PopupMenuWrapper menu) {
        trackSubscription.unsubscribe();
        trackSubscription = trackRepository
                .track(track.getEntityUrn())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TrackSubscriber(track, menu));
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
                playlistOperations.removeTrackFromPlaylist(removeTrackListener.getPlaylistUrn(), track.getEntityUrn())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new DefaultSubscriber<PropertySet>() {
                            @Override
                            public void onNext(PropertySet args) {
                                removeTrackListener.onPlaylistTrackRemoved(positionInAdapter);
                            }
                        });
                return true;
            case R.id.play_related_tracks:
                playRelatedTracksWithDelayedLoadingDialog(
                        context,
                        context.getString(R.string.loading_related_tracks),
                        context.getString(R.string.unable_to_play_related_tracks),
                        1
                );
                return true;
            case R.id.start_station:
                startStationPresenter.startStationForTrack(context, track.getEntityUrn());
                return true;
            default:
                return false;
        }
    }

    private void handleShare(Context context) {
        shareOperations.share(context, track.getSource(), ScreenElement.LIST.get(),
                screenProvider.getLastScreenTag(), pageUrn, getPromotedSource());
    }

    private void playRelatedTracksWithDelayedLoadingDialog(Context context, String loadingMessage, String onErrorToastText, int startPosition) {
        DelayedLoadingDialogPresenter delayedLoadingDialogPresenter = dialogBuilder
                .setLoadingMessage(loadingMessage)
                .setOnErrorToastText(onErrorToastText)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        relatedTracksPlaybackSubscription.unsubscribe();
                    }
                })
                .create()
                .show(context);

        relatedTracksPlaybackSubscription = playbackInitiator
                .playTrackWithRecommendations(track.getEntityUrn(), new PlaySessionSource(screenProvider.getLastScreenTag()), startPosition)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ExpandAndDismissDialogSubscriber(context, eventBus, playbackToastHelper, delayedLoadingDialogPresenter));
    }

    private void showAddToPlaylistDialog() {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(
                track.getEntityUrn(), track.getTitle(), ScreenElement.LIST.get(),
                screenProvider.getLastScreenTag());
        from.show(activity.getFragmentManager());
    }

    private void trackLike(boolean addLike) {
        final Urn trackUrn = track.getEntityUrn();

        eventBus.publish(EventQueue.TRACKING,
                UIEvent.fromToggleLike(addLike,
                        ScreenElement.LIST.get(),
                        screenProvider.getLastScreenTag(),
                        screenProvider.getLastScreenTag(),
                        trackUrn,
                        pageUrn,
                        getPromotedSource(),
                        PlayableMetadata.from(track)));
    }

    private void handleLike() {
        final Urn trackUrn = track.getEntityUrn();
        final boolean addLike = !track.isLiked();
        likeOperations.toggleLike(trackUrn, addLike)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LikeToggleSubscriber(context, addLike));

        trackLike(addLike);
    }

    private void handleRepost() {
        final Urn trackUrn = track.getEntityUrn();
        final boolean repost = !track.isReposted();
        repostOperations.toggleRepost(trackUrn, repost)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new RepostResultSubscriber(context, repost));
    }

    private static class TrackSubscriber extends DefaultSubscriber<PropertySet> {
        private final TrackItem track;
        private final PopupMenuWrapper menu;

        public TrackSubscriber(TrackItem track, PopupMenuWrapper menu) {
            this.track = track;
            this.menu = menu;
        }

        @Override
        public void onNext(PropertySet details) {
            track.update(details);
            updateLikeActionTitle(track.isLiked());
            updateRepostActionTitle(track.isReposted());
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
        return removeTrackListener != null && !removeTrackListener.getPlaylistUrn().equals(Urn.NOT_SET);
    }

    private boolean isTrackFromPromotedPlaylist() {
        return this.promotedSourceInfo != null && this.promotedSourceInfo.getPromotedItemUrn().isPlaylist();
    }

    private PromotedSourceInfo getPromotedSource() {
        return this.promotedSourceInfo;
    }

    private static class ExpandAndDismissDialogSubscriber extends ExpandPlayerSubscriber {

        private final Context context;
        private final DelayedLoadingDialogPresenter delayedLoadingDialogPresenter;

        public ExpandAndDismissDialogSubscriber(Context context,
                                                EventBus eventBus,
                                                PlaybackToastHelper playbackToastHelper,
                                                DelayedLoadingDialogPresenter delayedLoadingDialogPresenter) {
            super(eventBus, playbackToastHelper);
            this.context = context;
            this.delayedLoadingDialogPresenter = delayedLoadingDialogPresenter;
        }

        @Override
        public void onError(Throwable e) {
            delayedLoadingDialogPresenter.onError(context);
            // Call on error after dismissing the dialog in order to report errors to Fabric.
            super.onError(e);
        }

        @Override
        public void onNext(PlaybackResult result) {
            if (result.isSuccess()) {
                expandPlayer();
                delayedLoadingDialogPresenter.onSuccess();
            } else {
                delayedLoadingDialogPresenter.onError(context);
            }
        }
    }
}
