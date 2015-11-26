package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER;
import static com.soundcloud.android.offline.OfflineProperty.Collection;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class PlaylistEngagementsPresenter extends DefaultSupportFragmentLightCycle implements PlaylistEngagementsView.OnEngagementListener {

    private Context context;
    private PlaylistWithTracks playlistWithTracks;
    private OriginProvider originProvider;
    private PlaySessionSource playSessionSourceInfo = PlaySessionSource.EMPTY;

    private final RepostOperations repostOperations;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;
    private final LikeOperations likeOperations;
    private final PlaylistEngagementsView playlistEngagementsView;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineOperations;
    private final OfflinePlaybackOperations offlinePlaybackOperations;
    private final PlaybackToastHelper playbackToastHelper;
    private final Navigator navigator;
    private final ShareOperations shareOperations;

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();
    private CompositeSubscription offlineStateSubscription = new CompositeSubscription();

    @Inject
    public PlaylistEngagementsPresenter(EventBus eventBus,
                                        RepostOperations repostOperations,
                                        AccountOperations accountOperations,
                                        LikeOperations likeOperations,
                                        PlaylistEngagementsView playlistEngagementsView,
                                        FeatureOperations featureOperations,
                                        OfflineContentOperations offlineOperations,
                                        OfflinePlaybackOperations offlinePlaybackOperations,
                                        PlaybackToastHelper playbackToastHelper,
                                        Navigator navigator,
                                        ShareOperations shareOperations) {
        this.eventBus = eventBus;
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.likeOperations = likeOperations;
        this.playlistEngagementsView = playlistEngagementsView;
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineOperations;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.playbackToastHelper = playbackToastHelper;
        this.navigator = navigator;
        this.shareOperations = shareOperations;
    }

    @VisibleForTesting
    void bindView(View rootView) {
        bindView(rootView, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.UNKNOWN.get();
            }
        });
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    void bindView(View rootView, OriginProvider originProvider) {
        this.context = rootView.getContext();
        this.originProvider = originProvider;
        playlistEngagementsView.onViewCreated(rootView);
        playlistEngagementsView.setOnEngagement(this);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        playlistEngagementsView.onDestroyView();
    }

    @Override
    public void onResume(Fragment fragment) {
        foregroundSubscription = eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new PlaylistChangedSubscriber());
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
        offlineStateSubscription.unsubscribe();
    }

    @VisibleForTesting
    void setOriginProvider(OriginProvider originProvider) {
        this.originProvider = originProvider;
    }

    void setPlaylistInfo(@NotNull final PlaylistWithTracks playlistWithTracks, PlaySessionSource playSessionSource) {
        this.playlistWithTracks = playlistWithTracks;
        this.playSessionSourceInfo = playSessionSource;

        final String trackCount = context.getResources().getQuantityString(
                R.plurals.number_of_sounds, playlistWithTracks.getTrackCount(), playlistWithTracks.getTrackCount());
        playlistEngagementsView.setInfoText(context.getString(R.string.playlist_new_info_header_text_trackcount_duration,
                trackCount, playlistWithTracks.getDuration()));

        playlistEngagementsView.updateLikeItem(this.playlistWithTracks.getLikesCount(), this.playlistWithTracks.isLikedByUser());

        showPublicOptions(playlistWithTracks);
        showShuffleOption(playlistWithTracks);

        updateOfflineAvailability();
        subscribeForOfflineContentUpdates();
    }

    private void subscribeForOfflineContentUpdates() {
        if (featureOperations.isOfflineContentEnabled()) {
            offlineStateSubscription.unsubscribe();
            offlineStateSubscription = new CompositeSubscription();
            offlineStateSubscription.add(
                    eventBus.queue(EventQueue.CURRENT_DOWNLOAD)
                            .filter(isPlaylist(playlistWithTracks))
                            .map(CurrentDownloadEvent.TO_OFFLINE_STATE)
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(playlistWithTracks.getDownloadState())
                            .subscribe(new OfflineStateSubscriber()));

            offlineStateSubscription.add(
                    eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                            .filter(IS_PLAYLIST_OFFLINE_CONTENT_EVENT_FILTER)
                            .map(EntityStateChangedEvent.TO_SINGULAR_CHANGE)
                            .filter(isCurrentPlaylist(playlistWithTracks))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new OfflineStatusSubscriber()));
        }
    }

    private void showShuffleOption(PlaylistWithTracks playlistWithTracks) {
        if (playlistWithTracks.getTrackCount() > 1) {
            playlistEngagementsView.enableShuffle();
        } else {
            playlistEngagementsView.disableShuffle();
        }
    }

    private void showPublicOptions(PlaylistWithTracks playlistWithTracks) {
        if (playlistWithTracks.isPublic()) {
            boolean showRepost = !accountOperations.isLoggedInUser(playlistWithTracks.getCreatorUrn());
            if (showRepost) {
                playlistEngagementsView.showPublicOptions(this.playlistWithTracks.isRepostedByUser());
            } else {
                playlistEngagementsView.showPublicOptionsForYourTrack();
            }
        } else {
            playlistEngagementsView.hidePublicOptions();
        }
    }

    private Func1<? super PropertySet, Boolean> isCurrentPlaylist(final PlaylistWithTracks playlistWithTracks) {
        return new Func1<PropertySet, Boolean>() {
            @Override
            public Boolean call(PropertySet entityChange) {
                return entityChange.get(EntityProperty.URN).equals(playlistWithTracks.getUrn());
            }
        };
    }

    private Func1<CurrentDownloadEvent, Boolean> isPlaylist(final PlaylistWithTracks playlistWithTracks) {
        return new Func1<CurrentDownloadEvent, Boolean>() {
            @Override
            public Boolean call(CurrentDownloadEvent event) {
                return event.entities.contains(playlistWithTracks.getUrn());
            }
        };
    }

    private void updateOfflineAvailability() {
        if (featureOperations.isOfflineContentEnabled() && isEligibleForOfflineContent()) {
            playlistEngagementsView.setOfflineOptionsMenu(playlistWithTracks.isOfflineAvailable());
        } else if (featureOperations.upsellOfflineContent()) {
            playlistEngagementsView.showUpsell();
        } else {
            playlistEngagementsView.hideOfflineContentOptions();
        }
    }

    private boolean isEligibleForOfflineContent() {
        return playlistWithTracks.isPostedByUser() || playlistWithTracks.isLikedByUser();
    }

    @Override
    public void onMakeOfflineAvailable(boolean isMarkedForOffline) {
        Observable<Boolean> observable = isMarkedForOffline
                ? offlineOperations.makePlaylistAvailableOffline(playlistWithTracks.getUrn())
                : offlineOperations.makePlaylistUnavailableOffline(playlistWithTracks.getUrn());
        fireAndForget(observable);

        eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(isMarkedForOffline));
    }

    private TrackingEvent getOfflinePlaylistTrackingEvent(boolean isMarkedForOffline) {
        return isMarkedForOffline ?
                UIEvent.fromAddOfflinePlaylist(
                        Screen.PLAYLIST_DETAILS.get(),
                        playlistWithTracks.getUrn(),
                        playSessionSourceInfo.getPromotedSourceInfo()) :
                UIEvent.fromRemoveOfflinePlaylist(
                        Screen.PLAYLIST_DETAILS.get(),
                        playlistWithTracks.getUrn(),
                        playSessionSourceInfo.getPromotedSourceInfo());
    }

    @Override
    public void onUpsellImpression() {
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forPlaylistPageImpression());
    }

    @Override
    public void onUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forPlaylistPageClick());
    }

    @Override
    public void onPlayShuffled() {
        if (playlistWithTracks != null) {
            offlinePlaybackOperations
                    .playPlaylistShuffled(playlistWithTracks.getUrn(), playSessionSourceInfo)
                    .doOnCompleted(publishAnalyticsEventForShuffle())
                    .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
        }
    }

    private Action0 publishAnalyticsEventForShuffle() {
        return new Action0() {
            @Override
            public void call() {
                final UIEvent fromShufflePlaylist = UIEvent.fromShufflePlaylist(originProvider.getScreenTag(), playlistWithTracks.getUrn());
                eventBus.publish(EventQueue.TRACKING, fromShufflePlaylist);
            }
        };
    }

    @Override
    public void onToggleLike(boolean addLike) {
        if (playlistWithTracks != null) {
            eventBus.publish(EventQueue.TRACKING,
                    UIEvent.fromToggleLike(addLike,
                            playlistWithTracks.getUrn(),
                            getEventContext(),
                            playSessionSourceInfo.getPromotedSourceInfo(),
                            EntityMetadata.from(playlistWithTracks)));

            fireAndForget(likeOperations.toggleLike(playlistWithTracks.getUrn(), addLike));
        }
    }

    @Override
    public void onToggleRepost(boolean isReposted, boolean showResultToast) {
        if (playlistWithTracks != null) {
            eventBus.publish(EventQueue.TRACKING,
                    UIEvent.fromToggleRepost(isReposted,
                            playlistWithTracks.getUrn(),
                            getEventContext(),
                            playSessionSourceInfo.getPromotedSourceInfo(),
                            EntityMetadata.from(playlistWithTracks)));

            if (showResultToast) {
                repostOperations.toggleRepost(playlistWithTracks.getUrn(), isReposted)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSubscriber(context, isReposted));
            } else {
                fireAndForget(repostOperations.toggleRepost(playlistWithTracks.getUrn(), isReposted));
            }
        }
    }

    private EventContextMetadata getEventContext() {
        return EventContextMetadata.builder()
                .contextScreen(originProvider.getScreenTag())
                .pageName(Screen.PLAYLIST_DETAILS.get())
                .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                .pageUrn(playlistWithTracks.getUrn())
                .build();
    }

    @Override
    public void onShare() {
        if (playlistWithTracks != null) {
            shareOperations.share(context,
                    playlistWithTracks.getSourceSet(),
                    getEventContext(),
                    playSessionSourceInfo.getPromotedSourceInfo());
        }
    }

    private class PlaylistChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            if (playlistWithTracks != null && playlistWithTracks.getUrn().equals(event.getFirstUrn())) {
                final PropertySet changeSet = event.getNextChangeSet();
                playlistWithTracks.update(changeSet);

                if (changeSet.contains(PlaylistProperty.IS_LIKED)) {
                    playlistEngagementsView.updateLikeItem(
                            changeSet.get(PlayableProperty.LIKES_COUNT),
                            changeSet.get(PlayableProperty.IS_LIKED));
                }
                if (changeSet.contains(PlaylistProperty.IS_REPOSTED)) {
                    playlistEngagementsView.showPublicOptions(
                            changeSet.get(PlayableProperty.IS_REPOSTED));
                }
                if (changeSet.contains(Collection.IS_MARKED_FOR_OFFLINE)) {
                    updateOfflineAvailability();
                }
            }
        }
    }

    private class OfflineStateSubscriber extends DefaultSubscriber<OfflineState> {
        @Override
        public void onNext(OfflineState state) {
            playlistEngagementsView.showOfflineState(state);
        }
    }

    private class OfflineStatusSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet entityChanges) {
            if (!entityChanges.get(Collection.IS_MARKED_FOR_OFFLINE)) {
                playlistEngagementsView.showOfflineState(OfflineState.NO_OFFLINE);
            }
        }
    }
}
