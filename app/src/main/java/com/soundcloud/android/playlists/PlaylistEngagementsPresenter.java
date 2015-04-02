package com.soundcloud.android.playlists;

import static com.soundcloud.android.offline.OfflineProperty.Collection;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class PlaylistEngagementsPresenter extends DefaultSupportFragmentLightCycle implements PlaylistEngagementsView.OnEngagementListener {

    private static final String SHARE_TYPE = "text/plain";

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

    private Subscription foregroundSubscription = Subscriptions.empty();
    private Subscription offlineStateSubscription = Subscriptions.empty();

    @Inject
    public PlaylistEngagementsPresenter(EventBus eventBus,
                                        RepostOperations repostOperations,
                                        AccountOperations accountOperations,
                                        LikeOperations likeOperations,
                                        PlaylistEngagementsView playlistEngagementsView,
                                        FeatureOperations featureOperations,
                                        OfflineContentOperations offlineOperations,
                                        OfflinePlaybackOperations offlinePlaybackOperations,
                                        PlaybackToastHelper playbackToastHelper) {
        this.eventBus = eventBus;
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.likeOperations = likeOperations;
        this.playlistEngagementsView = playlistEngagementsView;
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineOperations;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.playbackToastHelper = playbackToastHelper;
    }

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

    void setOriginProvider(OriginProvider originProvider) {
        this.originProvider = originProvider;
    }

    void setPlaylistInfo(@NotNull PlaylistWithTracks playlistWithTracks, PlaySessionSource playSessionSource) {
        this.playlistWithTracks = playlistWithTracks;
        this.playSessionSourceInfo = playSessionSource;

        final String trackCount = context.getResources().getQuantityString(
                R.plurals.number_of_sounds, playlistWithTracks.getTrackCount(), playlistWithTracks.getTrackCount());
        playlistEngagementsView.setInfoText(context.getString(R.string.playlist_new_info_header_text,
                trackCount, playlistWithTracks.getDuration()));

        playlistEngagementsView.updateLikeItem(this.playlistWithTracks.getLikesCount(), this.playlistWithTracks.isLikedByUser());

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

        if (playlistWithTracks.getTrackCount() > 1){
            playlistEngagementsView.enableShuffle();
        } else {
            playlistEngagementsView.disableShuffle();
        }

        updateOfflineAvailability();
        offlineStateSubscription.unsubscribe();
        offlineStateSubscription = eventBus
                .queue(EventQueue.CURRENT_DOWNLOAD)
                .subscribe(new DownloadStateSubscriber());
    }

    private void updateOfflineAvailability() {
        if (featureOperations.isOfflineContentEnabled()) {
            playlistEngagementsView.setOfflineOptionsMenu(playlistWithTracks.isOfflineAvailable());
        } else if (featureOperations.isOfflineContentUpsellEnabled()) {
            playlistEngagementsView.showUpsell();
        } else {
            playlistEngagementsView.hideOfflineContentOptions();
        }
    }

    @Override
    public void onMakeOfflineAvailable(boolean isMarkedForOffline) {
        Observable<Boolean> observable = isMarkedForOffline
                ? offlineOperations.makePlaylistAvailableOffline(playlistWithTracks.getUrn())
                : offlineOperations.makePlaylistUnavailableOffline(playlistWithTracks.getUrn());
        fireAndForget(observable);
    }

    @Override
    public void onUpsell() {
        // No-op
    }

    @Override
    public void onPlayShuffled() {
        offlinePlaybackOperations
                .playPlaylistShuffled(playlistWithTracks.getUrn(), playSessionSourceInfo)
                .doOnCompleted(publishAnalyticsEventForShuffle())
                .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
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
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleLike(addLike,
                    Screen.PLAYLIST_DETAILS.get(),
                    PlaylistEngagementsPresenter.this.originProvider.getScreenTag(),
                    playlistWithTracks.getUrn()));

            fireAndForget(likeOperations.toggleLike(playlistWithTracks.getUrn(), addLike));
        }
    }

    @Override
    public void onToggleRepost(boolean isReposted, boolean showResultToast) {
        if (playlistWithTracks != null) {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleRepost(isReposted,
                    PlaylistEngagementsPresenter.this.originProvider.getScreenTag(), playlistWithTracks.getUrn()));
            if (showResultToast) {
                repostOperations.toggleRepost(playlistWithTracks.getUrn(), isReposted)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new RepostResultSubscriber(context, isReposted));
            } else {
                fireAndForget(repostOperations.toggleRepost(playlistWithTracks.getUrn(), isReposted));
            }
        }
    }

    @Override
    public void onShare() {
        if (playlistWithTracks != null) {
            eventBus.publish(EventQueue.TRACKING,
                    UIEvent.fromShare(PlaylistEngagementsPresenter.this.originProvider.getScreenTag(), playlistWithTracks.getUrn()));
            sendShareIntent();
        }
    }

    private void sendShareIntent() {
        if (playlistWithTracks.isPrivate()) {
            return;
        }
        context.startActivity(buildShareIntent());
    }

    private Intent buildShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, playlistWithTracks.getTitle()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildShareIntentText());
        return shareIntent;
    }

    private String buildShareIntentText() {
        if (ScTextUtils.isNotBlank(playlistWithTracks.getCreatorName())) {
            return context.getString(R.string.share_track_by_artist_on_soundcloud,
                    playlistWithTracks.getTitle(), playlistWithTracks.getCreatorName(), playlistWithTracks.getPermalinkUrl());
        }
        return context.getString(R.string.share_track_on_soundcloud, playlistWithTracks.getTitle(), playlistWithTracks.getPermalinkUrl());
    }

    private class PlaylistChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            if (playlistWithTracks != null && playlistWithTracks.getUrn().equals(event.getNextUrn())) {
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

    private class DownloadStateSubscriber extends DefaultSubscriber<CurrentDownloadEvent> {
        @Override
        public void onNext(CurrentDownloadEvent event) {
            if (playlistWithTracks != null && event.entities.contains(playlistWithTracks.getUrn())) {
                switch (event.kind) {
                    case DOWNLOADED:
                        playlistEngagementsView.showDownloadedState();
                        break;
                    case DOWNLOADING:
                        playlistEngagementsView.showDownloadingState();
                        break;
                    case REQUESTED:
                        playlistEngagementsView.showDefaultState();
                        break;
                    case NO_OFFLINE:
                        playlistEngagementsView.showDefaultState();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown state:" + event.kind);
                }
            }
        }
    }
}
