package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
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
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
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
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class PlaylistViewHeaderPresenter extends DefaultSupportFragmentLightCycle<Fragment> implements PlaylistEngagementsView.OnEngagementListener {

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
    private final NetworkConnectionHelper connectionHelper;
    private final OfflineSettingsOperations offlineSettings;

    private Context context;
    private PlaylistHeaderListener listener;
    private FragmentManager fragmentManager;
    private Subscription offlineStateSubscription = RxUtils.invalidSubscription();
    private PlaylistHeaderItem playlistHeaderItem;
    private String screen;

    @Inject
    public PlaylistViewHeaderPresenter(RepostOperations repostOperations,
                                       AccountOperations accountOperations,
                                       EventBus eventBus,
                                       LikeOperations likeOperations,
                                       PlaylistEngagementsView playlistEngagementsView,
                                       FeatureOperations featureOperations,
                                       OfflineContentOperations offlineOperations,
                                       PlaybackInitiator playbackInitiator,
                                       PlaylistOperations playlistOperations,
                                       PlaybackToastHelper playbackToastHelper,
                                       Navigator navigator,
                                       ShareOperations shareOperations,
                                       NetworkConnectionHelper connectionHelper,
                                       OfflineSettingsOperations offlineSettings) {
        this.repostOperations = repostOperations;
        this.accountOperations = accountOperations;
        this.eventBus = eventBus;
        this.likeOperations = likeOperations;
        this.playlistEngagementsView = playlistEngagementsView;
        this.featureOperations = featureOperations;
        this.offlineOperations = offlineOperations;
        this.playbackInitiator = playbackInitiator;
        this.playlistOperations = playlistOperations;
        this.playbackToastHelper = playbackToastHelper;
        this.navigator = navigator;
        this.shareOperations = shareOperations;
        this.connectionHelper = connectionHelper;
        this.offlineSettings = offlineSettings;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        context = fragment.getContext();

        // TODO
        if (featureOperations.upsellOfflineContent()) {
            Urn playlistUrn = fragment.getArguments().getParcelable(LegacyPlaylistDetailFragment.EXTRA_URN);
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forPlaylistPageImpression(playlistUrn));
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        fragmentManager = fragment.getFragmentManager();
    }

    @Override
    public void onPause(Fragment fragment) {
        offlineStateSubscription.unsubscribe();
        fragmentManager = null;
    }

    public void bindView(View view) {
        playlistEngagementsView.onViewCreated(view);
        playlistEngagementsView.setOnEngagementListener(this);
    }

    public void update(EntityStateChangedEvent event) {
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

    void setScreen(final String screen) {
        this.screen = screen;
    }

    @NonNull
    View.OnClickListener getCreatorClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playlistHeaderItem != null) {
                    listener.onGoToCreator(playlistHeaderItem.getCreatorUrn());
                }
            }
        };
    }

    @NonNull
    View.OnClickListener getPlayButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onHeaderPlay();
            }
        };
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        playlistEngagementsView.onDestroyView();
    }

    void setPlaylistInfo(@NotNull final PlaylistHeaderItem playlistHeaderItem) {
        this.playlistHeaderItem = playlistHeaderItem;

        final String trackCount = context.getResources().getQuantityString(
                R.plurals.number_of_sounds, playlistHeaderItem.getTrackCount(),
                playlistHeaderItem.getTrackCount());

        playlistEngagementsView.setInfoText(context.getString(R.string.playlist_new_info_header_text_trackcount_duration,
                trackCount, playlistHeaderItem.geFormattedDuration()));

        playlistEngagementsView.updateLikeItem(playlistHeaderItem.getLikesCount(), playlistHeaderItem.isLikedByUser());

        showPublicOptions();
        showShuffleOption();

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

    private void showShuffleOption() {
        if (playlistHeaderItem.getTrackCount() > 1) {
            playlistEngagementsView.enableShuffle();
        } else {
            playlistEngagementsView.disableShuffle();
        }
    }

    private void showPublicOptions() {
        if (playlistHeaderItem.isPublic()) {
            if (isOwned(playlistHeaderItem)) {
                playlistEngagementsView.showPublicOptionsForYourTrack();
            } else {
                playlistEngagementsView.showPublicOptions(this.playlistHeaderItem.isRepostedByUser());
            }
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
            playlistEngagementsView.showMakeAvailableOfflineButton(isPlaylistOfflineAvailable);
        } else if (featureOperations.upsellOfflineContent()) {
            playlistEngagementsView.showUpsell();
        } else {
            playlistEngagementsView.hideMakeAvailableOfflineButton();
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
                    playlistHeaderItem.getPlaySessionSource().getPromotedSourceInfo());
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
                UpgradeTrackingEvent.forPlaylistPageClick(playlistHeaderItem.getUrn()));
    }

    @Override
    public void onPlayShuffled() {
        if (playlistHeaderItem != null) {
            final Observable<List<Urn>> tracks = playlistOperations.trackUrnsForPlayback(playlistHeaderItem.getUrn());
            playbackInitiator.playTracksShuffled(tracks, playlistHeaderItem.getPlaySessionSource(), featureOperations.isOfflineContentEnabled())
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
        listener.onEditPlaylist();
    }

    private Action0 publishAnalyticsEventForShuffle() {
        return new Action0() {
            @Override
            public void call() {
                final UIEvent fromShufflePlaylist = UIEvent.fromShufflePlaylist(screen, playlistHeaderItem.getUrn());
                eventBus.publish(EventQueue.TRACKING, fromShufflePlaylist);
            }
        };
    }

    @Override
    public void onToggleLike(boolean addLike) {
        if (playlistHeaderItem != null) {
            eventBus.publish(EventQueue.TRACKING,
                    UIEvent.fromToggleLike(addLike,
                            playlistHeaderItem.getUrn(),
                            getEventContext(),
                            playlistHeaderItem.getPlaySessionSource().getPromotedSourceInfo(),
                            EntityMetadata.from(playlistHeaderItem)));

            fireAndForget(likeOperations.toggleLike(playlistHeaderItem.getUrn(), addLike));
        }
    }

    @Override
    public void onToggleRepost(boolean isReposted, boolean showResultToast) {
        if (playlistHeaderItem != null) {
            eventBus.publish(EventQueue.TRACKING,
                    UIEvent.fromToggleRepost(isReposted,
                            playlistHeaderItem.getUrn(),
                            getEventContext(),
                            playlistHeaderItem.getPlaySessionSource().getPromotedSourceInfo(),
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
                .contextScreen(screen)
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

    public void setListener(PlaylistHeaderListener listener) {
        this.listener = listener;
    }
}
