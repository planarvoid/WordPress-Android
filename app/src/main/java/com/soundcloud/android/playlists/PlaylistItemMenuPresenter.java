package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.likes.LikeToggleSubscriber;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.view.View;

import javax.inject.Inject;

public class PlaylistItemMenuPresenter implements PlaylistItemMenuRenderer.Listener {

    private final Context appContext;
    private final EventBus eventBus;
    private final PlaylistOperations playlistOperations;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final ShareOperations shareOperations;
    private final ScreenProvider screenProvider;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final Navigator navigator;
    private final PlayQueueHelper playQueueHelper;
    private final EventTracker eventTracker;
    private final PlaylistItemMenuRendererFactory playlistItemMenuRendererFactory;

    private Subscription playlistSubscription = RxUtils.invalidSubscription();
    private OverflowMenuOptions menuOptions;
    private Optional<EventContextMetadata.Builder> eventContextMetadataBuilder;
    private Urn playlistUrn;

    private Optional<PromotedSourceInfo> promotedSourceInfo;
    private EntityMetadata entityMetadata;
    private PlaylistItemMenuRenderer renderer;

    @Inject
    public PlaylistItemMenuPresenter(Context appContext,
                                     EventBus eventBus,
                                     PlaylistOperations playlistOperations,
                                     LikeOperations likeOperations,
                                     RepostOperations repostOperations,
                                     ShareOperations shareOperations,
                                     ScreenProvider screenProvider,
                                     FeatureOperations featureOperations,
                                     OfflineContentOperations offlineContentOperations,
                                     Navigator navigator,
                                     PlayQueueHelper playQueueHelper,
                                     EventTracker eventTracker,
                                     PlaylistItemMenuRendererFactory playlistItemMenuRendererFactory) {
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.playlistOperations = playlistOperations;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.shareOperations = shareOperations;
        this.screenProvider = screenProvider;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.navigator = navigator;
        this.playQueueHelper = playQueueHelper;
        this.eventTracker = eventTracker;
        this.playlistItemMenuRendererFactory = playlistItemMenuRendererFactory;
    }

    public void show(View button, PlaylistItem playlist, OverflowMenuOptions menuOptions) {
        show(button, playlist, menuOptions, null);
    }

    public void show(View button, Urn playlistUrn, OverflowMenuOptions menuOptions) {
        this.menuOptions = menuOptions;
        this.eventContextMetadataBuilder = Optional.absent();
        this.renderer = playlistItemMenuRendererFactory.create(this, button, menuOptions);
        this.playlistUrn = playlistUrn;
        this.promotedSourceInfo = Optional.absent();
        this.entityMetadata = EntityMetadata.EMPTY;
        loadPlaylist(playlistUrn);
    }

    public void show(View button,
                     PlaylistItem playlist,
                     OverflowMenuOptions menuOptions,
                     EventContextMetadata.Builder eventContextMetadataBuilder) {

        this.menuOptions = menuOptions;
        this.eventContextMetadataBuilder = Optional.fromNullable(eventContextMetadataBuilder);
        renderer = playlistItemMenuRendererFactory.create(this, button, menuOptions);
        playlistUrn = playlist.getUrn();
        promotedSourceInfo = loadPromotedSourceInfo(playlist);
        entityMetadata = EntityMetadata.from(playlist);
        loadPlaylist(playlistUrn);
    }

    private Optional<PromotedSourceInfo> loadPromotedSourceInfo(PlaylistItem playlist) {
        if (playlist instanceof PromotedPlaylistItem) {
            return Optional.of(PromotedSourceInfo.fromItem((PromotedPlaylistItem) playlist));
        }
        return Optional.absent();
    }

    @Override
    public void onDismiss() {
        playlistSubscription.unsubscribe();
        playlistSubscription = Subscriptions.empty();
    }

    public void handlePlayNext() {
        playQueueHelper.playNext(playlistUrn);
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayNext(playlistUrn,
                                                                   screenProvider.getLastScreenTag(),
                                                                   getEventContextMetadata()));
    }

    private static FragmentManager toFragmentManager(Context context) {
        return getFragmentActivity(context).getSupportFragmentManager();
    }

    public void saveOffline() {
        fireAndForget(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn));
        eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.fromAddOfflinePlaylist(
                screenProvider.getLastScreenTag(),
                playlistUrn,
                promotedSourceInfo.orNull()));
    }

    public void removeFromOffline(Context context) {
        if (offlineContentOperations.isOfflineCollectionEnabled()) {
            ConfirmRemoveOfflineDialogFragment.showForPlaylist(toFragmentManager(context),
                                                               playlistUrn,
                                                               promotedSourceInfo.orNull());
        } else {
            fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn));
            eventBus.publish(EventQueue.TRACKING, OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                    screenProvider.getLastScreenTag(),
                    playlistUrn,
                    promotedSourceInfo.orNull()));
        }
    }

    public void deletePlaylist(Context context) {
        DeletePlaylistDialogFragment.show(toFragmentManager(context), playlistUrn);
    }

    private EventContextMetadata getEventContextMetadata() {
        final EventContextMetadata.Builder builder = eventContextMetadataBuilder.isPresent() ?
                                                     eventContextMetadataBuilder.get() :
                                                     EventContextMetadata.builder()
                                                                         .invokerScreen(ScreenElement.LIST.get())
                                                                         .contextScreen(screenProvider.getLastScreenTag())
                                                                         .pageName(screenProvider.getLastScreenTag());

        return builder.isFromOverflow(true).build();
    }

    public void handleLike(PlaylistItem playlist) {
        boolean addLike = !playlist.isLiked();
        likeOperations.toggleLike(playlistUrn, addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new LikeToggleSubscriber(appContext, addLike));

        eventTracker.trackEngagement(
                UIEvent.fromToggleLike(addLike,
                                       playlistUrn,
                                       getEventContextMetadata(),
                                       promotedSourceInfo.orNull(),
                                       entityMetadata));

        if (isUnlikingNotOwnedPlaylistInOfflineMode(addLike, playlist)) {
            fireAndForget(offlineContentOperations.makePlaylistUnavailableOffline(playlistUrn));
        }
    }

    public void handleRepost(boolean addRepost) {
        repostOperations.toggleRepost(playlistUrn, addRepost)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSubscriber(appContext, addRepost));

        eventTracker.trackEngagement(
                UIEvent.fromToggleRepost(addRepost,
                                         playlistUrn,
                                         getEventContextMetadata(),
                                         promotedSourceInfo.orNull(),
                                         entityMetadata));
    }

    @Override
    public void handleUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING,
                         UpgradeFunnelEvent.forPlaylistItemClick(screenProvider.getLastScreenTag(),
                                                                 playlistUrn));
    }

    public void handleShare(Context context, PlaylistItem playlist) {
        final PropertySet playable = playlist.getSource();
        final boolean isPublic = !playable.get(PlayableProperty.IS_PRIVATE);

        if (isPublic) {
            shareOperations.share(context,
                                  playable.get(PlayableProperty.PERMALINK_URL),
                                  getEventContextMetadata(), promotedSourceInfo.orNull(),
                                  EntityMetadata.from(playable));
        }
    }

    private boolean isUnlikingNotOwnedPlaylistInOfflineMode(boolean addLike, PlaylistItem playlist) {
        boolean offlineContentEnabled = featureOperations.isOfflineContentEnabled() && menuOptions.showOffline();
        return offlineContentEnabled && !addLike && !playlist.isPostedByUser();
    }

    // this is really ugly. We should introduce a PlaylistRepository.
    // https://github.com/soundcloud/SoundCloud-Android/issues/2942
    private void loadPlaylist(Urn urn) {
        playlistSubscription.unsubscribe();
        playlistSubscription = playlistOperations
                .playlist(urn)
                .first()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaylistSubscriber());
    }

    private final class PlaylistSubscriber extends DefaultSubscriber<PlaylistWithTracks> {

        @Override
        public void onNext(PlaylistWithTracks details) {
            final PlaylistItem playlistItem = PlaylistItem.from(details.getSourceSet());
            renderer.render(playlistItem);
        }
    }

}
