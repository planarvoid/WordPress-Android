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
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.playqueue.PlayQueueHelper;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class PlaylistHeaderPresenter extends SupportFragmentLightCycleDispatcher<Fragment>
        implements CellRenderer<PlaylistDetailsHeaderItem>, PlaylistDetailsInputs {

    private final EventBus eventBus;
    private final EventTracker eventTracker;
    private final Navigator navigator;
    private final PlaylistEngagementsRenderer playlistEngagementsRenderer;
    private final OfflineContentOperations offlineOperations;
    private final PlaybackInitiator playbackInitiator;
    private final PlaylistOperations playlistOperations;
    private final PlaybackToastHelper playbackToastHelper;
    private final LikeOperations likeOperations;
    private final RepostOperations repostOperations;
    private final SharePresenter sharePresenter;
    private final PlayQueueHelper playQueueHelper;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private FeatureFlags featureFlags;
    private final PlaylistCoverRenderer playlistCoverRenderer;
    private final FeatureOperations featureOperations;
    private final AccountOperations accountOperations;

    @LightCycle final PlaylistHeaderScrollHelper playlistHeaderScrollHelper;

    private Subscription foregroundSubscription = RxUtils.invalidSubscription();
    private Subscription offlineStateSubscription = RxUtils.invalidSubscription();
    private FragmentManager fragmentManager;
    private Activity activity;
    private PlaylistHeaderPresenterListener playlistHeaderPresenterListener;
    private PlaylistDetailsViewModel viewModel;
    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;
    private Optional<View> headerView = Optional.absent();
    private String screen;

    @Inject
    PlaylistHeaderPresenter(EventBus eventBus,
                            EventTracker eventTracker,
                            Navigator navigator,
                            PlaylistHeaderScrollHelper playlistHeaderScrollHelper,
                            FeatureOperations featureOperations,
                            OfflineContentOperations offlineOperations,
                            PlaybackInitiator playbackInitiator,
                            PlaylistOperations playlistOperations,
                            PlaybackToastHelper playbackToastHelper,
                            LikeOperations likeOperations,
                            RepostOperations repostOperations,
                            SharePresenter sharePresenter,
                            OfflinePropertiesProvider offlinePropertiesProvider,
                            PlayQueueHelper playQueueHelper,
                            PlaylistCoverRenderer playlistCoverRenderer,
                            PlaylistEngagementsRenderer playlistEngagementsRenderer,
                            FeatureFlags featureFlags,
                            AccountOperations accountOperations) {
        this.eventBus = eventBus;
        this.eventTracker = eventTracker;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.playlistHeaderScrollHelper = playlistHeaderScrollHelper;
        this.playlistCoverRenderer = playlistCoverRenderer;
        this.playlistEngagementsRenderer = playlistEngagementsRenderer;
        this.offlineOperations = offlineOperations;
        this.playbackInitiator = playbackInitiator;
        this.playlistOperations = playlistOperations;
        this.playbackToastHelper = playbackToastHelper;
        this.likeOperations = likeOperations;
        this.repostOperations = repostOperations;
        this.sharePresenter = sharePresenter;
        this.playQueueHelper = playQueueHelper;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.accountOperations = accountOperations;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);

        if (featureOperations.upsellOfflineContent()) {
            Urn playlistUrn = fragment.getArguments().getParcelable(PlaylistDetailFragment.EXTRA_URN);
            if (playlistUrn != null) {
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageImpression(playlistUrn));
            }
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
                                                                   .filter(event -> viewModel != null && event.changeMap().containsKey(viewModel.metadata().getUrn()))
                                                                   .subscribe(event -> updateMetadata((PlaylistDetailsMetadata) event.apply(viewModel.metadata()))),
                                                           eventBus.subscribe(EventQueue.LIKE_CHANGED, new PlaylistLikesSubscriber()),
                                                           eventBus.subscribe(EventQueue.REPOST_CHANGED, new PlaylistRepostsSubscriber()));
        subscribeForOfflineContentUpdates();
    }

    private void updateMetadata(PlaylistDetailsMetadata metadata) {
        viewModel = viewModel.toBuilder().metadata(metadata).build();
    }

    @Override
    public void onPause(Fragment fragment) {
        foregroundSubscription.unsubscribe();
        offlineStateSubscription.unsubscribe();
        fragmentManager = null;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_details_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailsHeaderItem> items) {
        headerView = Optional.of(itemView);
        bindItemView();
    }

    void setPlaylistHeaderPresenterListener(PlaylistHeaderPresenterListener playlistHeaderPresenterListener) {
        this.playlistHeaderPresenterListener = playlistHeaderPresenterListener;
    }

    void setScreen(final String screen) {
        this.screen = screen;
    }

    void setPlaylist(PlaylistDetailsViewModel model, PlaySessionSource playSessionSource) {
        this.viewModel = model;
        this.playSessionSource = playSessionSource;
        bindItemView();
    }

    private void bindItemView() {
        if (headerView.isPresent() && viewModel != null) {
            playlistCoverRenderer.bind(headerView.get(), viewModel.metadata(), this::onHeaderPlay, this::onGoToCreator);
            bingEngagementBars();
        }
    }

    private void onHeaderPlay() {
        playlistHeaderPresenterListener.onHeaderPlayButtonClicked();
    }

    private void onGoToCreator() {
        playlistHeaderPresenterListener.onCreatorClicked();
    }

    private void bingEngagementBars() {
        playlistEngagementsRenderer.bind(headerView.get(), this, viewModel.metadata());
    }

    private void subscribeForOfflineContentUpdates() {
        offlineStateSubscription.unsubscribe();
        final Observable<OfflineState> offlineState;

        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            offlineState = offlinePropertiesProvider
                    .states()
                    .filter(event -> viewModel != null)
                    .map(properties -> properties.state(viewModel.metadata().getUrn()));
        } else {
            offlineState = eventBus.queue(EventQueue.OFFLINE_CONTENT_CHANGED)
                                   .filter(event -> viewModel != null && event.entities.contains(viewModel.metadata().getUrn()))
                                   .map(OfflineContentChangedEvent.TO_OFFLINE_STATE);

        }

        offlineStateSubscription = offlineState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OfflineStateSubscriber());
    }

    @Override
    public void onMakeOfflineAvailable() {
        if (viewModel.metadata().isLikedByUser() || isPlaylistOwnedByCurrentUser()) {
            saveOffline();
        } else {
            likeAndSaveOffline();
        }

        eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(true));
    }

    @Override
    public void onMakeOfflineUnavailable() {
        if (offlineOperations.isOfflineCollectionEnabled()) {
            ConfirmRemoveOfflineDialogFragment.showForPlaylist(fragmentManager, viewModel.metadata().getUrn(),
                                                               playSessionSource.getPromotedSourceInfo());
        } else {
            fireAndForget(offlineOperations.makePlaylistUnavailableOffline(viewModel.metadata().getUrn()));
            eventBus.publish(EventQueue.TRACKING, getOfflinePlaylistTrackingEvent(false));
        }
    }

    @Override
    public void onMakeOfflineUpsell() {
        navigator.openUpgrade(activity);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageClick(viewModel.metadata().getUrn()));
    }

    private TrackingEvent getOfflinePlaylistTrackingEvent(boolean isMarkedForOffline) {
        return isMarkedForOffline ?
               OfflineInteractionEvent.fromAddOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       viewModel.metadata().getUrn(),
                       playSessionSource.getPromotedSourceInfo()) :
               OfflineInteractionEvent.fromRemoveOfflinePlaylist(
                       Screen.PLAYLIST_DETAILS.get(),
                       viewModel.metadata().getUrn(),
                       playSessionSource.getPromotedSourceInfo());
    }


    @Override
    public void onItemTriggered(PlaylistDetailUpsellItem item) {
        navigator.openUpgrade(activity);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistPageClick(viewModel.metadata().getUrn()));
    }

    @Override
    public void onOverflowUpsell() {
        navigator.openUpgrade(activity);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowClick(viewModel.metadata().getUrn()));
    }

    @Override
    public void onOverflowUpsellImpression() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forPlaylistOverflowImpression(viewModel.metadata().getUrn()));
    }

    @Override
    public void onPlayShuffled() {
        if (viewModel != null) {
            final Observable<List<Urn>> tracks = playlistOperations.trackUrnsForPlayback(viewModel.metadata().getUrn());
            playbackInitiator
                    .playTracksShuffled(tracks, playSessionSource)
                    .doOnCompleted(() -> eventBus.publish(EventQueue.TRACKING, UIEvent.fromShuffle(getEventContext())))
                    .subscribe(new ShowPlayerSubscriber(eventBus, playbackToastHelper));
        }
    }

    @Override
    public void onDeletePlaylist() {
        DeletePlaylistDialogFragment.show(fragmentManager, viewModel.metadata().getUrn());
    }

    @Override
    public void onHeaderPlayButtonClicked() {

    }

    @Override
    public void onCreatorClicked() {

    }

    @Override
    public void onItemTriggered(PlaylistDetailTrackItem item) {

    }

    @Override
    public void onEnterEditMode() {
        playlistHeaderPresenterListener.onEnterEditMode();
    }

    @Override
    public void onExitEditMode() {

    }

    @Override
    public void onPlayNext() {
        playQueueHelper.playNext(viewModel.metadata().getUrn());
    }

    @Override
    public void onToggleLike(boolean addLike) {
        if (viewModel != null) {
            eventTracker.trackEngagement(UIEvent.fromToggleLike(addLike,
                                                                viewModel.metadata().getUrn(),
                                                                getEventContext(),
                                                                playSessionSource.getPromotedSourceInfo(),
                                                                createEntityMetadata()));

            fireAndForget(likeOperations.toggleLike(viewModel.metadata().getUrn(), addLike));
        }
    }

    private EntityMetadata createEntityMetadata() {
        return EntityMetadata.from(viewModel.metadata().creatorName(), viewModel.metadata().creatorUrn(),
                                   viewModel.metadata().title(), viewModel.metadata().getUrn());
    }

    @Override
    public void onToggleRepost(boolean isReposted) {
        toggleRepost(isReposted, true);
    }

    @VisibleForTesting
        // horrible things being done here. Deleting this class soon
    void toggleRepost(boolean isReposted, boolean showResultToast) {
        if (viewModel != null) {
            eventTracker.trackEngagement(UIEvent.fromToggleRepost(isReposted,
                                                                  viewModel.metadata().getUrn(),
                                                                  getEventContext(),
                                                                  playSessionSource
                                                                          .getPromotedSourceInfo(),
                                                                  createEntityMetadata()));

            if (showResultToast) {
                repostOperations.toggleRepost(viewModel.metadata().getUrn(), isReposted)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new RepostResultSubscriber(activity));
            } else {
                fireAndForget(repostOperations.toggleRepost(viewModel.metadata().getUrn(), isReposted));
            }
        }
    }

    private EventContextMetadata getEventContext() {
        return EventContextMetadata.builder()
                                   .contextScreen(screen)
                                   .pageName(Screen.PLAYLIST_DETAILS.get())
                                   .invokerScreen(Screen.PLAYLIST_DETAILS.get())
                                   .pageUrn(viewModel.metadata().getUrn())
                                   .build();
    }

    @Override
    public void onShareClicked() {
        if (viewModel != null) {
            final Optional<String> permalinkUrl = viewModel.metadata().permalinkUrl();
            if (!viewModel.metadata().isPrivate() && permalinkUrl.isPresent()) {
                sharePresenter.share(activity,
                                     permalinkUrl.get(),
                                     getEventContext(),
                                     playSessionSource.getPromotedSourceInfo(),
                                     createEntityMetadata());
            }
        }
    }

    private boolean isPlaylistOwnedByCurrentUser() {
        return accountOperations.isLoggedInUser(viewModel.metadata().creatorUrn());
    }

    private void likeAndSaveOffline() {
        final boolean addLike = true;
        fireAndForget(likeOperations.toggleLike(viewModel.metadata().getUrn(), addLike)
                      .observeOn(AndroidSchedulers.mainThread())
                      .doOnNext(ignored -> saveOffline()));
    }

    private void saveOffline() {
        fireAndForget(offlineOperations.makePlaylistAvailableOffline(viewModel.metadata().getUrn()));
    }

    private class OfflineStateSubscriber extends DefaultSubscriber<OfflineState> {
        @Override
        public void onNext(OfflineState state) {
            if (viewModel != null) {
                updateMetadata(viewModel.metadata()
                                        .toBuilder()
                                        .offlineState(state)
                                        .isMarkedForOffline(state != OfflineState.NOT_OFFLINE)
                                        .build());

                bindItemView();
            }
        }
    }

    private class PlaylistLikesSubscriber extends DefaultSubscriber<LikesStatusEvent> {
        @Override
        public void onNext(LikesStatusEvent event) {
            if (viewModel != null) {
                final Optional<LikesStatusEvent.LikeStatus> likeStatus = event.likeStatusForUrn(viewModel.metadata().getUrn());
                if (likeStatus.isPresent()) {
                    updateMetadata(viewModel.metadata().updatedWithLikeStatus(likeStatus.get()));
                    ;
                    bindItemView();
                }
            }
        }
    }

    private class PlaylistRepostsSubscriber extends DefaultSubscriber<RepostsStatusEvent> {
        @Override
        public void onNext(RepostsStatusEvent event) {
            if (viewModel != null) {
                final Optional<RepostsStatusEvent.RepostStatus> repostStatus = event.repostStatusForUrn(viewModel.metadata().getUrn());
                if (repostStatus.isPresent()) {
                    updateMetadata(viewModel.metadata().updatedWithRepostStatus(repostStatus.get()));
                    bindItemView();
                }
            }
        }
    }
}
