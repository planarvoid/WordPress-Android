package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
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
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.utils.NetworkConnectionHelper;
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

public class LegacyPlaylistEngagementsPresenter extends DefaultSupportFragmentLightCycle<Fragment>
        implements PlaylistEngagementsView.OnEngagementListener {

    private Context context;
    private FragmentManager fragmentManager;
    private PlaylistHeaderItem playlistHeaderItem;
    private OriginProvider originProvider;

    private final RepostOperations repostOperations;
    private final AccountOperations accountOperations;
    private final EventBus eventBus;
    private final EventTracker eventTracker;
    private final LikeOperations likeOperations;
    private final PlaylistEngagementsView playlistEngagementsView;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineOperations;
    private final PlaybackInitiator playbackInitiator;
    private final PlaylistOperations playlistOperations;
    private final PlaybackToastHelper playbackToastHelper;
    private final Navigator navigator;
    private final ShareOperations shareOperations;
    private final NetworkConnectionHelper connectionHelper;
    private final OfflineSettingsOperations offlineSettings;
    private final PlayQueueHelper playQueueHelper;

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();
    private Subscription offlineStateSubscription = RxUtils.invalidSubscription();

    private View rootView;

    @Inject
    LegacyPlaylistEngagementsPresenter(EventBus eventBus,
                                       EventTracker eventTracker,
                                       RepostOperations repostOperations,
                                       AccountOperations accountOperations,
                                       LikeOperations likeOperations,
                                       PlaylistEngagementsView playlistEngagementsView,
                                       FeatureOperations featureOperations,
                                       OfflineContentOperations offlineOperations,
                                       PlaybackInitiator playbackInitiator,
                                       PlaylistOperations playlistOperations,
                                       PlaybackToastHelper playbackToastHelper,
                                       NetworkConnectionHelper connectionHelper,
                                       OfflineSettingsOperations offlineSettings,
                                       Navigator navigator,
                                       ShareOperations shareOperations,
                                       PlayQueueHelper playQueueHelper) {
        this.eventBus = eventBus;
        this.eventTracker = eventTracker;
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.likeOperations = likeOperations;
        this.playlistEngagementsView = playlistEngagementsView;
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineOperations;
        this.playbackInitiator = playbackInitiator;
        this.playlistOperations = playlistOperations;
        this.playbackToastHelper = playbackToastHelper;
        this.connectionHelper = connectionHelper;
        this.offlineSettings = offlineSettings;
        this.navigator = navigator;
        this.shareOperations = shareOperations;
        this.playQueueHelper = playQueueHelper;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        if (featureOperations.upsellOfflineContent()) {
            Urn playlistUrn = fragment.getArguments().getParcelable(LegacyPlaylistDetailFragment.EXTRA_URN);
            if (playlistUrn != null) {
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageImpression(playlistUrn));
            }
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
        this.rootView = rootView;
        this.context = rootView.getContext();
        this.originProvider = originProvider;
        playlistEngagementsView.bindView(rootView);
        playlistEngagementsView.setOnEngagementListener(this);
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

    void setPlaylistInfo(@NotNull final PlaylistHeaderItem playlistHeaderItem) {
        this.playlistHeaderItem = playlistHeaderItem;

        playlistEngagementsView.bindView(rootView, playlistHeaderItem, false);

        final String trackCount = context.getResources().getQuantityString(
                R.plurals.number_of_sounds, this.playlistHeaderItem.getTrackCount(),
                this.playlistHeaderItem.getTrackCount());

        playlistEngagementsView.setInfoText(context.getString(R.string.playlist_new_info_header_text_trackcount_duration,
                                                              trackCount,
                                                              this.playlistHeaderItem.geFormattedDuration()));

        playlistEngagementsView.updateLikeItem(this.playlistHeaderItem.getLikesCount(),
                                               this.playlistHeaderItem.isLikedByUser());

        toggleMyOptions();
        togglePublicOptions();
        toggleShuffleOption();

        updateOfflineAvailability();
        subscribeForOfflineContentUpdates();
    }

    private void subscribeForOfflineContentUpdates() {
        offlineStateSubscription.unsubscribe();
        offlineStateSubscription = eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                                           .filter(isCurrentPlaylist(playlistHeaderItem))
                                           .map(OfflineContentChangedEvent.TO_OFFLINE_STATE)
                                           .observeOn(AndroidSchedulers.mainThread())
                                           .subscribe(new OfflineStateSubscriber());
    }

    private void toggleShuffleOption() {
        if (playlistHeaderItem.getTrackCount() > 1) {
            playlistEngagementsView.enableShuffle();
        } else {
            playlistEngagementsView.disableShuffle();
        }
    }

    private void toggleMyOptions() {
        if (isOwned(playlistHeaderItem)) {
            playlistEngagementsView.showMyOptions();
        } else {
            playlistEngagementsView.hideMyOptions();
        }
    }

    private void togglePublicOptions() {
        if (playlistHeaderItem.isPublic()) {
            playlistEngagementsView.showPublicOptions(playlistHeaderItem.isRepostedByUser());
        } else {
            playlistEngagementsView.hidePublicOptions();
        }
    }

    private boolean isOwned(PlaylistHeaderItem headerItem) {
        return accountOperations.isLoggedInUser(headerItem.getCreatorUrn());
    }

    private Func1<OfflineContentChangedEvent, Boolean> isCurrentPlaylist(final PlaylistHeaderItem headerItem) {
        return new Func1<OfflineContentChangedEvent, Boolean>() {
            @Override
            public Boolean call(OfflineContentChangedEvent event) {
                return event.entities.contains(headerItem.getUrn());
            }
        };
    }

    private void updateOfflineAvailability() {
        updateOfflineAvailability(playlistHeaderItem.isMarkedForOffline().or(false));
        playlistEngagementsView.showOfflineState(playlistHeaderItem.getDownloadState());
    }

    private void updateOfflineAvailability(boolean isPlaylistOfflineAvailable) {
        if (featureOperations.isOfflineContentEnabled() && isEligibleForOfflineContent()) {
            playlistEngagementsView.showOfflineOptions(isPlaylistOfflineAvailable);
        } else if (featureOperations.upsellOfflineContent()) {
            playlistEngagementsView.showUpsell();
        } else {
            playlistEngagementsView.hideOfflineOptions();
        }
    }

    private boolean isEligibleForOfflineContent() {
        return playlistHeaderItem.isPostedByUser() || playlistHeaderItem.isLikedByUser();
    }

    @Override
    public void onMakeOfflineAvailable(boolean isMarkedForOffline) {
        if (isMarkedForOffline) {
            fireAndForget(offlineOperations.makePlaylistAvailableOffline(playlistHeaderItem.getUrn()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(true));
        } else if (offlineOperations.isOfflineCollectionEnabled()) {
            playlistEngagementsView.setOfflineAvailability(true);
            ConfirmRemoveOfflineDialogFragment.showForPlaylist(fragmentManager, playlistHeaderItem.getUrn(),
                                                               playlistHeaderItem.getPlaySessionSource()
                                                                                 .getPromotedSourceInfo());
        } else {
            fireAndForget(offlineOperations.makePlaylistUnavailableOffline(playlistHeaderItem.getUrn()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(false));
        }
    }

    private TrackingEvent getOfflinePlaylistTrackingEvent(boolean isMarkedForOffline) {
        return isMarkedForOffline ?
               OfflineInteractionEvent.fromAddOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       playlistHeaderItem.getUrn(),
                       playlistHeaderItem.getPlaySessionSource().getPromotedSourceInfo()) :
               OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       playlistHeaderItem.getUrn(),
                       playlistHeaderItem.getPlaySessionSource().getPromotedSourceInfo());
    }

    @Override
    public void onUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING,
                         UpgradeFunnelEvent.forPlaylistPageClick(playlistHeaderItem.getUrn()));
    }

    @Override
    public void onOverflowUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING,
                UpgradeFunnelEvent.forPlaylistOverflowClick(playlistHeaderItem.getUrn()));
    }

    @Override
    public void onOverflowUpsellImpression() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowImpression(playlistHeaderItem.getUrn()));
    }

    @Override
    public void onPlayShuffled() {
        if (playlistHeaderItem != null) {
            final Observable<List<Urn>> tracks = playlistOperations.trackUrnsForPlayback(playlistHeaderItem.getUrn());
            playbackInitiator.playTracksShuffled(tracks, playlistHeaderItem.getPlaySessionSource())
                             .doOnCompleted(publishAnalyticsEventForShuffle())
                             .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
        }
    }

    @Override
    public void onDeletePlaylist() {
        DeletePlaylistDialogFragment.show(fragmentManager, playlistHeaderItem.getUrn());
    }

    @Override
    public void onEditPlaylist() {
        throw new UnsupportedOperationException("Edit playlist is not supported.");
    }

    private Action0 publishAnalyticsEventForShuffle() {
        return new Action0() {
            @Override
            public void call() {
                final UIEvent fromShufflePlaylist = UIEvent.fromShuffle(getEventContext());
                eventBus.publish(EventQueue.TRACKING, fromShufflePlaylist);
            }
        };
    }

    @Override
    public void onPlayNext(Urn playlistUrn) {
        playQueueHelper.playNext(playlistUrn);
    }

    @Override
    public void onToggleLike(boolean addLike) {
        if (playlistHeaderItem != null) {
            eventTracker.trackEngagement(UIEvent.fromToggleLike(addLike,
                                                                playlistHeaderItem.getUrn(),
                                                                getEventContext(),
                                                                playlistHeaderItem.getPlaySessionSource()
                                                                                  .getPromotedSourceInfo(),
                                                                EntityMetadata.from(playlistHeaderItem)));

            fireAndForget(likeOperations.toggleLike(playlistHeaderItem.getUrn(), addLike));
        }
    }

    @Override
    public void onToggleRepost(boolean isReposted, boolean showResultToast) {
        if (playlistHeaderItem != null) {
            eventTracker.trackEngagement(UIEvent.fromToggleRepost(isReposted,
                                                                  playlistHeaderItem.getUrn(),
                                                                  getEventContext(),
                                                                  playlistHeaderItem.getPlaySessionSource()
                                                                                    .getPromotedSourceInfo(),
                                                                  EntityMetadata.from(playlistHeaderItem)));

            if (showResultToast) {
                repostOperations.toggleRepost(playlistHeaderItem.getUrn(), isReposted)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new RepostResultSubscriber(context, isReposted));
            } else {
                fireAndForget(repostOperations.toggleRepost(playlistHeaderItem.getUrn(), isReposted));
            }
        }
    }

    private EventContextMetadata getEventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(originProvider.getScreenTag())
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
                                   .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                   .pageUrn(playlistHeaderItem.getUrn())
                                   .build();
    }

    @Override
    public void onShare() {
        if (playlistHeaderItem != null) {
            shareOperations.share(context,
                                  playlistHeaderItem.getSource(),
                                  getEventContext(),
                                  playlistHeaderItem.getPlaySessionSource().getPromotedSourceInfo());
        }
    }

    private class PlaylistChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            if (playlistHeaderItem != null && playlistHeaderItem.getUrn().equals(event.getFirstUrn())) {
                final PropertySet changeSet = event.getNextChangeSet();
                playlistHeaderItem.update(changeSet);

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

            if (state == OfflineState.REQUESTED) {
                showConnectionWarningIfNecessary();
            }
        }
    }

    private void showConnectionWarningIfNecessary() {
        if (offlineSettings.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected()) {
            playlistEngagementsView.showNoWifi();
        } else if (!connectionHelper.isNetworkConnected()) {
            playlistEngagementsView.showNoConnection();
        }
    }


}
