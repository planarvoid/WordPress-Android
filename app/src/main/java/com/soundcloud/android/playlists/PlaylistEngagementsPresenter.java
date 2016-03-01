package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class PlaylistEngagementsPresenter extends DefaultSupportFragmentLightCycle<Fragment>
        implements PlaylistEngagementsView.OnEngagementListener {

    private Context context;
    private FragmentManager fragmentManager;
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
    private final PlaybackInitiator playbackInitiator;
    private final PlaylistOperations playlistOperations;
    private final PlaybackToastHelper playbackToastHelper;
    private final Navigator navigator;
    private final ShareOperations shareOperations;

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();
    private Subscription offlineStateSubscription = RxUtils.invalidSubscription();

    @Inject
    public PlaylistEngagementsPresenter(EventBus eventBus,
                                        RepostOperations repostOperations,
                                        AccountOperations accountOperations,
                                        LikeOperations likeOperations,
                                        PlaylistEngagementsView playlistEngagementsView,
                                        FeatureOperations featureOperations,
                                        OfflineContentOperations offlineOperations,
                                        PlaybackInitiator playbackInitiator,
                                        PlaylistOperations playlistOperations, PlaybackToastHelper playbackToastHelper,
                                        Navigator navigator,
                                        ShareOperations shareOperations) {
        this.eventBus = eventBus;
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.likeOperations = likeOperations;
        this.playlistEngagementsView = playlistEngagementsView;
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineOperations;
        this.playbackInitiator = playbackInitiator;
        this.playlistOperations = playlistOperations;
        this.playbackToastHelper = playbackToastHelper;
        this.navigator = navigator;
        this.shareOperations = shareOperations;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        if (featureOperations.upsellOfflineContent()) {
            Urn playlistUrn = fragment.getArguments().getParcelable(PlaylistDetailFragment.EXTRA_URN);
            eventBus.publish(EventQueue.TRACKING,
                    UpgradeTrackingEvent.forPlaylistPageImpression(playlistUrn));
        }
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
        fragmentManager = fragment.getFragmentManager();
        foregroundSubscription = eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new PlaylistChangedSubscriber());
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
        offlineStateSubscription.unsubscribe();
        fragmentManager = null;
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
        offlineStateSubscription.unsubscribe();
        offlineStateSubscription = eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                .filter(isCurrentPlaylist(playlistWithTracks))
                .map(OfflineContentChangedEvent.TO_OFFLINE_STATE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OfflineStateSubscriber());
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
            if (isOwned(playlistWithTracks)) {
                playlistEngagementsView.showPublicOptionsForYourTrack();
            } else {
                playlistEngagementsView.showPublicOptions(this.playlistWithTracks.isRepostedByUser());
            }
        } else {
            playlistEngagementsView.hidePublicOptions();
        }
    }

    private boolean isOwned(PlaylistWithTracks playlistWithTracks) {
        return accountOperations.isLoggedInUser(playlistWithTracks.getCreatorUrn());
    }

    private Func1<OfflineContentChangedEvent, Boolean> isCurrentPlaylist(final PlaylistWithTracks playlistWithTracks) {
        return new Func1<OfflineContentChangedEvent, Boolean>() {
            @Override
            public Boolean call(OfflineContentChangedEvent event) {
                return event.entities.contains(playlistWithTracks.getUrn());
            }
        };
    }

    private void updateOfflineAvailability() {
        updateOfflineAvailability(playlistWithTracks.isOfflineAvailable());
        playlistEngagementsView.showOfflineState(playlistWithTracks.getDownloadState());
    }

    private void updateOfflineAvailability(boolean isPlaylistOfflineAvailable) {
        if (featureOperations.isOfflineContentEnabled() && isEligibleForOfflineContent()) {
            playlistEngagementsView.showMakeAvailableOfflineButton(isPlaylistOfflineAvailable);
        } else if (featureOperations.upsellOfflineContent()) {
            playlistEngagementsView.showUpsell();
        } else {
            playlistEngagementsView.hideMakeAvailableOfflineButton();
        }
    }

    private boolean isEligibleForOfflineContent() {
        return playlistWithTracks.isPostedByUser() || playlistWithTracks.isLikedByUser();
    }

    @Override
    public void onMakeOfflineAvailable(boolean isMarkedForOffline) {
        if (isMarkedForOffline) {
            fireAndForget(offlineOperations.makePlaylistAvailableOffline(playlistWithTracks.getUrn()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(true));
        } else if (offlineOperations.isOfflineCollectionEnabled()) {
            playlistEngagementsView.setOfflineAvailability(true);
            ConfirmRemoveOfflineDialogFragment.showForPlaylist(fragmentManager, playlistWithTracks.getUrn(), playSessionSourceInfo.getPromotedSourceInfo());
        } else {
            fireAndForget(offlineOperations.makePlaylistUnavailableOffline(playlistWithTracks.getUrn()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(false));
        }
    }

    private TrackingEvent getOfflinePlaylistTrackingEvent(boolean isMarkedForOffline) {
        return isMarkedForOffline ?
                OfflineInteractionEvent.fromAddOfflinePlaylist(
                        Screen.PLAYLIST_DETAILS.get(),
                        playlistWithTracks.getUrn(),
                        playSessionSourceInfo.getPromotedSourceInfo()) :
                OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                        Screen.PLAYLIST_DETAILS.get(),
                        playlistWithTracks.getUrn(),
                        playSessionSourceInfo.getPromotedSourceInfo());
    }

    @Override
    public void onUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING,
                UpgradeTrackingEvent.forPlaylistPageClick(playlistWithTracks.getUrn()));
    }

    @Override
    public void onPlayShuffled() {
        if (playlistWithTracks != null) {
            final Observable<List<Urn>> tracks = playlistOperations.trackUrnsForPlayback(playlistWithTracks.getUrn());
            playbackInitiator.playTracksShuffled(tracks, playSessionSourceInfo, featureOperations.isOfflineContentEnabled())
                    .doOnCompleted(publishAnalyticsEventForShuffle())
                    .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
        }
    }

    @Override
    public void onDeletePlaylist() {
        DeletePlaylistDialogFragment.show(fragmentManager, playlistWithTracks.getUrn());
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

                if (changeSet.contains(PlaylistProperty.IS_USER_LIKE)) {
                    playlistEngagementsView.updateLikeItem(
                            changeSet.get(PlayableProperty.LIKES_COUNT),
                            changeSet.get(PlayableProperty.IS_USER_LIKE));
                    updateOfflineAvailability();
                }
                if (changeSet.contains(PlaylistProperty.IS_USER_REPOST)) {
                    playlistEngagementsView.showPublicOptions(
                            changeSet.get(PlayableProperty.IS_USER_REPOST));
                }
            }
        }
    }

    private class OfflineStateSubscriber extends DefaultSubscriber<OfflineState> {
        @Override
        public void onNext(OfflineState state) {
            updateOfflineAvailability(state != OfflineState.NOT_OFFLINE);
            playlistEngagementsView.showOfflineState(state);
        }
    }

}
