package com.soundcloud.android.playlists;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class PlaylistHeaderPresenter extends SupportFragmentLightCycleDispatcher<Fragment>
        implements PlaylistEngagementsView.OnEngagementListener, CellRenderer<PlaylistDetailHeaderItem> {

    private final EventBus eventBus;
    private final EventTracker eventTracker;
    private final PlaylistHeaderViewFactory playlistDetailsViewFactory;
    private final Navigator navigator;
    private final FeatureOperations featureOperations;
    private final PlaylistEngagementsView playlistEngagementsView;
    private final AccountOperations accountOperations;
    private final OfflineContentOperations offlineOperations;
    private final PlaybackInitiator playbackInitiator;
    private final PlaylistOperations playlistOperations;
    private final PlaybackToastHelper playbackToastHelper;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final ShareOperations shareOperations;
    private final OfflineSettingsOperations offlineSettings;
    private final NetworkConnectionHelper connectionHelper;
    private final PlayQueueHelper playQueueHelper;

    @LightCycle final PlaylistHeaderScrollHelper playlistHeaderScrollHelper;

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();
    private Subscription offlineStateSubscription = RxUtils.invalidSubscription();
    private FragmentManager fragmentManager;
    private Activity activity;
    private PlaylistPresenter playlistPresenter;
    private boolean isEditMode = false;
    private PlaylistWithTracks playlistWithTracks;
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
    private Optional<View> headerView = Optional.absent();
    private String screen;

    @Inject
    PlaylistHeaderPresenter(EventBus eventBus,
                            EventTracker eventTracker,
                            PlaylistHeaderViewFactory playlistDetailsViewFactory,
                            Navigator navigator,
                            PlaylistHeaderScrollHelper playlistHeaderScrollHelper,
                            FeatureOperations featureOperations,
                            PlaylistEngagementsView playlistEngagementsView,
                            AccountOperations accountOperations,
                            OfflineContentOperations offlineOperations,
                            PlaybackInitiator playbackInitiator,
                            PlaylistOperations playlistOperations,
                            PlaybackToastHelper playbackToastHelper,
                            LikeOperations likeOperations,
                            RepostOperations repostOperations,
                            ShareOperations shareOperations,
                            OfflineSettingsOperations offlineSettings,
                            NetworkConnectionHelper connectionHelper,
                            PlayQueueHelper playQueueHelper) {
        this.eventBus = eventBus;
        this.eventTracker = eventTracker;
        this.playlistDetailsViewFactory = playlistDetailsViewFactory;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.playlistHeaderScrollHelper = playlistHeaderScrollHelper;
        this.playlistEngagementsView = playlistEngagementsView;
        this.accountOperations = accountOperations;
        this.offlineOperations = offlineOperations;
        this.playbackInitiator = playbackInitiator;
        this.playlistOperations = playlistOperations;
        this.playbackToastHelper = playbackToastHelper;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.shareOperations = shareOperations;
        this.offlineSettings = offlineSettings;
        this.connectionHelper = connectionHelper;
        this.playQueueHelper = playQueueHelper;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);

        if (featureOperations.upsellOfflineContent()) {
            Urn playlistUrn = fragment.getArguments().getParcelable(PlaylistDetailFragment.EXTRA_URN);
            eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageImpression(playlistUrn));
        }
    }

    @Override
    public void onAttach(Fragment fragment, Activity activity) {
        super.onAttach(fragment, activity);
        this.activity = activity;
    }

    @Override
    public void onDetach(Fragment fragment) {
        this.activity = null;
        super.onDetach(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        View headerView = view.findViewById(R.id.playlist_details);
        if (headerView != null) {
            this.headerView = Optional.of(headerView);
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        fragmentManager = fragment.getFragmentManager();
        foregroundSubscription = new CompositeSubscription(eventBus.queue(EventQueue.PLAYLIST_CHANGED)
                                                                   .filter(event -> playlistWithTracks != null && event.changeMap().containsKey(playlistWithTracks.getUrn()))
                                                                   .subscribe(event -> {
                                                                       playlistWithTracks = (PlaylistWithTracks) event.apply(playlistWithTracks);
                                                                   }),
                                                           eventBus.subscribe(EventQueue.LIKE_CHANGED, new PlaylistLikesSubscriber()),
                                                           eventBus.subscribe(EventQueue.REPOST_CHANGED, new PlaylistRepostsSubscriber()));
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
        offlineStateSubscription.unsubscribe();
        fragmentManager = null;
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        playlistEngagementsView.onDestroyView();
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_details_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailHeaderItem> items) {
        headerView = Optional.of(itemView);
        bindItemView();
    }

    void setPlaylistPresenter(PlaylistPresenter playlistPresenter) {
        this.playlistPresenter = playlistPresenter;
    }

    void setScreen(final String screen) {
        this.screen = screen;
    }

    void setPlaylist(PlaylistWithTracks headerItem, PlaySessionSource playSessionSource) {
        this.playlistWithTracks = headerItem;
        this.playSessionSource = playSessionSource;
        bindItemView();
    }

    void setEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
        bindItemView();
    }

    private void bindItemView() {
        if (headerView.isPresent() && playlistWithTracks != null) {
            bindPlaylistHeader();
            bingEngagementBars();
        }
    }

    private void bindPlaylistHeader() {
        if (headerView.isPresent()) {
            PlaylistHeaderView playlistDetailsView = playlistDetailsViewFactory.create(headerView.get());
            playlistDetailsView.setOnPlayButtonClickListener(v -> onHeaderPlay());
            playlistDetailsView.setOnCreatorButtonClickListener(view -> {
                if (playlistWithTracks != null) {
                    onGoToCreator(view, playlistWithTracks.getCreatorUrn());
                }
            });

            final Playlist playlist = playlistWithTracks.getPlaylist();
            playlistDetailsView.setPlaylist(PlaylistItem.from(playlist), shouldShowPlayButton(isEditMode));
        }
    }

    private boolean shouldShowPlayButton(boolean isEditMode) {
        return !isEditMode && !playlistWithTracks.getTracks().isEmpty();
    }

    private void onHeaderPlay() {
        playlistPresenter.playFromBegninning();
    }

    private void onGoToCreator(View view, Urn creatorUrn) {
        navigator.legacyOpenProfile(view.getContext(), creatorUrn);
    }

    @Override
    public void onEditPlaylist() {
        playlistPresenter.setEditMode(true);
    }

    private void bingEngagementBars() {
        updateEngagementBar();
        toggleMyOptions();
        togglePublicOptions();
        toggleShuffleOption();

        updateOfflineAvailability();
        subscribeForOfflineContentUpdates();
    }

    private void updateEngagementBar() {
        if (headerView.isPresent()) {
            playlistEngagementsView.bindView(headerView.get(), playlistWithTracks, isEditMode);
            playlistEngagementsView.updateLikeItem(Optional.of(playlistWithTracks.getLikesCount()), playlistWithTracks.isLikedByUser());
            playlistEngagementsView.setOnEngagementListener(this);
        }
    }

    private void subscribeForOfflineContentUpdates() {
        offlineStateSubscription.unsubscribe();
        offlineStateSubscription = eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                                           .filter(event -> event.entities.contains(playlistWithTracks.getUrn()))
                                           .map(OfflineContentChangedEvent.TO_OFFLINE_STATE)
                                           .observeOn(AndroidSchedulers.mainThread())
                                           .subscribe(new OfflineStateSubscriber());
    }

    private void toggleShuffleOption() {
        if (playlistWithTracks.getTrackCount() > 1) {
            playlistEngagementsView.enableShuffle();
        } else {
            playlistEngagementsView.disableShuffle();
        }
    }

    private void toggleMyOptions() {
        if (isOwned(playlistWithTracks)) {
            playlistEngagementsView.showMyOptions();
        } else {
            playlistEngagementsView.hideMyOptions();
        }
    }

    private void togglePublicOptions() {
        if (playlistWithTracks.isPublic()) {
            playlistEngagementsView.showPublicOptions(playlistWithTracks.isRepostedByUser());
        } else {
            playlistEngagementsView.hidePublicOptions();
        }
    }

    private boolean isOwned(PlaylistWithTracks headerItem) {
        return accountOperations.isLoggedInUser(headerItem.getCreatorUrn());
    }

    private void updateOfflineAvailability() {
        updateOfflineAvailability(playlistWithTracks.isMarkedForOffline().or(false));
        playlistEngagementsView.showOfflineState(playlistWithTracks.getDownloadState());
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
        return accountOperations.isLoggedInUser(playlistWithTracks.getCreatorUrn()) || playlistWithTracks.isLikedByUser();
    }

    @Override
    public void onMakeOfflineAvailable(boolean isMarkedForOffline) {
        if (isMarkedForOffline) {
            fireAndForget(offlineOperations.makePlaylistAvailableOffline(playlistWithTracks.getUrn()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(true));
        } else if (offlineOperations.isOfflineCollectionEnabled()) {
            playlistEngagementsView.setOfflineAvailability(true);
            ConfirmRemoveOfflineDialogFragment.showForPlaylist(fragmentManager, playlistWithTracks.getUrn(),
                                                               playSessionSource.getPromotedSourceInfo());
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
                       playSessionSource.getPromotedSourceInfo()) :
               OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       playlistWithTracks.getUrn(),
                       playSessionSource.getPromotedSourceInfo());
    }

    @Override
    public void onUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageClick(playlistWithTracks.getUrn()));
    }

    @Override
    public void onOverflowUpsell(Context context) {
        navigator.openUpgrade(context);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowClick(playlistWithTracks.getUrn()));
    }

    @Override
    public void onOverflowUpsellImpression() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowImpression(playlistWithTracks.getUrn()));
    }

    @Override
    public void onPlayShuffled() {
        if (playlistWithTracks != null) {
            final Observable<List<Urn>> tracks = playlistOperations.trackUrnsForPlayback(playlistWithTracks.getUrn());
            playbackInitiator
                    .playTracksShuffled(tracks, playSessionSource)
                    .doOnCompleted(() -> eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffle(getEventContext())))
                    .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
        }
    }

    @Override
    public void onDeletePlaylist() {
        DeletePlaylistDialogFragment.show(fragmentManager, playlistWithTracks.getUrn());
    }

    @Override
    public void onPlayNext(Urn playlistUrn) {
        playQueueHelper.playNext(playlistUrn);
    }

    @Override
    public void onToggleLike(boolean addLike) {
        if (playlistWithTracks != null) {
            eventTracker.trackEngagement(UIEvent.fromToggleLike(addLike,
                                                                playlistWithTracks.getUrn(),
                                                                getEventContext(),
                                                                playSessionSource
                                                                        .getPromotedSourceInfo(),
                                                                EntityMetadata.from(playlistWithTracks)));

            fireAndForget(likeOperations.toggleLike(playlistWithTracks.getUrn(), addLike));
        }
    }

    @Override
    public void onToggleRepost(boolean isReposted, boolean showResultToast) {
        if (playlistWithTracks != null) {
            eventTracker.trackEngagement(UIEvent.fromToggleRepost(isReposted,
                                                                  playlistWithTracks.getUrn(),
                                                                  getEventContext(),
                                                                  playSessionSource
                                                                          .getPromotedSourceInfo(),
                                                                  EntityMetadata.from(playlistWithTracks)));

            if (showResultToast) {
                repostOperations.toggleRepost(playlistWithTracks.getUrn(), isReposted)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new RepostResultSubscriber(activity, isReposted));
            } else {
                fireAndForget(repostOperations.toggleRepost(playlistWithTracks.getUrn(), isReposted));
            }
        }
    }

    private EventContextMetadata getEventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(screen)
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
                                   .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                   .pageUrn(playlistWithTracks.getUrn())
                                   .build();
    }

    @Override
    public void onShare() {
        if (playlistWithTracks != null && !playlistWithTracks.isPrivate()) {
            shareOperations.share(activity,
                                  playlistWithTracks.getPermalinkUrl(),
                                  getEventContext(),
                                  playSessionSource.getPromotedSourceInfo(),
                                  EntityMetadata.from(playlistWithTracks));
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

    private class PlaylistLikesSubscriber extends DefaultSubscriber<LikesStatusEvent> {
        @Override
        public void onNext(LikesStatusEvent event) {
            if (playlistWithTracks != null) {
                final Optional<LikesStatusEvent.LikeStatus> likeStatus = event.likeStatusForUrn(playlistWithTracks.getUrn());
                if (likeStatus.isPresent()) {
                    playlistWithTracks = playlistWithTracks.updatedWithLikeStatus(likeStatus.get());
                    playlistEngagementsView.updateLikeItem(likeStatus.get().likeCount(), likeStatus.get().isUserLike());
                    updateOfflineAvailability();
                }
            }
        }
    }

    private class PlaylistRepostsSubscriber extends DefaultSubscriber<RepostsStatusEvent> {
        @Override
        public void onNext(RepostsStatusEvent event) {
            if (playlistWithTracks != null) {
                final Optional<RepostsStatusEvent.RepostStatus> repostStatus = event.repostStatusForUrn(playlistWithTracks.getUrn());
                if (repostStatus.isPresent()) {
                    playlistWithTracks = playlistWithTracks.updatedWithRepostStatus(repostStatus.get());
                    playlistEngagementsView.showPublicOptions(repostStatus.get().isReposted());
                }
            }
        }
    }
}
