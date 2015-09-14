package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.PlaylistOperations.PlaylistMissingException;
import static rx.android.app.AppObservable.bindSupportFragment;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ShowPlayerSubscriber;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.rx.eventbus.EventBus;
import org.w3c.dom.Text;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
@SuppressWarnings("PMD.TooManyFields")
public class PlaylistDetailFragment extends LightCycleSupportFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, PlaylistDetailsController.Listener {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";

    @Inject PlaylistDetailsController.Provider controllerProvider;
    @Inject PlaylistOperations playlistOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject OfflinePlaybackOperations offlinePlaybackOperations;
    @Inject ImageOperations imageOperations;
    @Inject @LightCycle PlaylistEngagementsPresenter engagementsPresenter;
    @Inject @LightCycle PullToRefreshController pullToRefreshController;
    @Inject PlayQueueManager playQueueManager;
    @Inject EventBus eventBus;
    @Inject PlaylistPresenter playlistPresenter;
    @Inject PlaybackToastHelper playbackToastHelper;
    @Inject Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    @Inject FeatureFlags featureFlags;
    @Inject AccountOperations accountOperations;
    @Inject Navigator navigator;
    @Inject FeatureOperations featureOperations;

    private PlaylistDetailsController controller;

    private ListView listView;
    private View progressView;

    private Observable<PlaylistWithTracks> loadPlaylist;
    private Subscription playlistSubscription = RxUtils.invalidSubscription();
    private CompositeSubscription eventSubscription = new CompositeSubscription();

    private View headerUsernameText;
    private ToggleButton playToggle;
    private PlaylistWithTracks playlistWithTracks;

    private boolean listShown;
    private boolean playOnLoad;

    private final View.OnClickListener onPlayToggleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (playbackOperations.shouldDisableSkipping()) {
                playToggle.setChecked(false);
            }

            if (playQueueManager.isCurrentCollection(playlistWithTracks.getUrn())) {
                playbackOperations.togglePlayback();
            } else {
                playFromBeginning();
            }
        }
    };

    private final View.OnClickListener onHeaderTextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openProfile(getActivity(), playlistWithTracks.getCreatorUrn());
        }
    };

    private final DefaultSubscriber<Player.StateTransition> playstateTransitionSubscriber = new DefaultSubscriber<Player.StateTransition>() {
        @Override
        public void onNext(Player.StateTransition event) {
            playToggle.setChecked(playQueueManager.isCurrentCollection(getPlaylistUrn())
                    && event.playSessionIsActive());
        }
    };

    private final DefaultSubscriber<EntityStateChangedEvent> trackAddedToPlaylist = new DefaultSubscriber<EntityStateChangedEvent>() {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            if (event.getFirstUrn().equals(playlistWithTracks.getUrn())) {
                onPlaylistContentChanged();
            }
        }
    };

    public static PlaylistDetailFragment create(Urn playlistUrn, Screen screen, SearchQuerySourceInfo searchInfo, PromotedSourceInfo promotedInfo) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_URN, playlistUrn);
        bundle.putParcelable(EXTRA_QUERY_SOURCE_INFO, searchInfo);
        bundle.putParcelable(EXTRA_PROMOTED_SOURCE_INFO, promotedInfo);
        screen.addToBundle(bundle);
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public PlaylistDetailFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    PlaylistDetailFragment(PlaylistDetailsController.Provider controllerProvider,
                           PlaybackOperations playbackOperations,
                           OfflinePlaybackOperations offlinePlaybackOperations,
                           PlaylistOperations playlistOperations,
                           EventBus eventBus,
                           ImageOperations imageOperations,
                           PlaylistEngagementsPresenter engagementsPresenter,
                           PullToRefreshController pullToRefreshController,
                           PlayQueueManager playQueueManager,
                           PlaylistPresenter playlistPresenter,
                           Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                           FeatureFlags featureFlags,
                           FeatureOperations featureOperations,
                           AccountOperations accountOperations,
                           Navigator navigator) {
        this.controllerProvider = controllerProvider;
        this.playbackOperations = playbackOperations;
        this.playlistOperations = playlistOperations;
        this.offlinePlaybackOperations = offlinePlaybackOperations;
        this.eventBus = eventBus;
        this.imageOperations = imageOperations;
        this.engagementsPresenter = engagementsPresenter;
        this.pullToRefreshController = pullToRefreshController;
        this.playQueueManager = playQueueManager;
        this.playlistPresenter = playlistPresenter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.featureFlags = featureFlags;
        this.featureOperations = featureOperations;
        this.accountOperations = accountOperations;
        this.navigator = navigator;
        addLifeCycleComponents();
    }

    private void playFromBeginning() {
        playTracksAtPosition(0, new ShowPlayerAfterPlaybackSubscriber(eventBus));
    }

    private void addLifeCycleComponents() {
        pullToRefreshController.setRefreshListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLoadPlaylistObservable();
        if (savedInstanceState == null) {
            playOnLoad = getActivity().getIntent().getBooleanExtra(PlaylistDetailActivity.EXTRA_AUTO_PLAY, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        controller = controllerProvider.create();
        return inflater.inflate(R.layout.playlist_fragment, container, false);
    }

    @Override
    public void onViewCreated(View layout, Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);

        controller.onViewCreated(layout, savedInstanceState);

        progressView = layout.findViewById(R.id.progress_container);

        listView = (ListView) layout.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true));

        configureInfoViews(layout);
        listView.setAdapter(controller.getAdapter());

        showContent(listShown);

        subscribeToLoadObservable();
    }

    private void createLoadPlaylistObservable() {
        loadPlaylist = bindSupportFragment(this, playlistOperations.playlist(getPlaylistUrn()));
    }

    private void subscribeToLoadObservable() {
        playlistSubscription.unsubscribe();
        playlistSubscription = loadPlaylist.subscribe(new PlaylistSubscriber());
    }

    @Override
    public void onRefresh() {
        playlistSubscription = playlistOperations.updatedPlaylistInfo(getPlaylistUrn())
                .observeOn(mainThread())
                .subscribe(new RefreshSubscriber());
    }

    @Override
    public void onResume() {
        super.onResume();
        eventSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED,
                playstateTransitionSubscriber));
        eventSubscription.add(
                eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                        .filter(EntityStateChangedEvent.IS_TRACK_ADDED_TO_PLAYLIST_FILTER)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(trackAddedToPlaylist));
    }

    @Override
    public void onPause() {
        eventSubscription.unsubscribe();
        eventSubscription = new CompositeSubscription();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        playlistSubscription.unsubscribe();
        controller.onDestroyView();
        super.onDestroyView();
    }

    private Urn getPlaylistUrn() {
        // if possible, use the instance to get the ID as it can change during syncing
        if (playlistWithTracks != null) {
            return playlistWithTracks.getUrn();
        }
        return getArguments().getParcelable(EXTRA_URN);
    }

    private SearchQuerySourceInfo getSearchQuerySourceInfo() {
        return getArguments().getParcelable(EXTRA_QUERY_SOURCE_INFO);
    }

    private PromotedSourceInfo getPromotedSourceInfo() {
        return getArguments().getParcelable(EXTRA_PROMOTED_SOURCE_INFO);
    }

    private void configureInfoViews(View layout) {
        View details = layout.findViewById(R.id.playlist_details);
        if (details == null) {
            details = createDetailsHeader();
        }
        setupPlaylistDetails(details);
    }

    private View createDetailsHeader() {
        View headerView = View.inflate(getActivity(), R.layout.playlist_details_view, null);
        listView.addHeaderView(headerView, null, false);
        return headerView;
    }

    private void setupPlaylistDetails(View detailsView) {
        playlistPresenter
                .setTitleView((TextView) detailsView.findViewById(R.id.title))
                .setPrivateTitleView((TextView) detailsView.findViewById(R.id.title_private))
                .setUsernameView((TextView) detailsView.findViewById(R.id.username))
                .setArtwork((ImageView) detailsView.findViewById(R.id.artwork),
                        ApiImageSize.getFullImageSize(getActivity().getResources()));

        engagementsPresenter.bindView(detailsView, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.fromBundle(getArguments()).get();
            }
        });

        playToggle = (ToggleButton) detailsView.findViewById(R.id.toggle_play_pause);
        playToggle.setOnClickListener(onPlayToggleClick);

        headerUsernameText = detailsView.findViewById(R.id.username);
        headerUsernameText.setOnClickListener(onHeaderTextClick);
    }

    private void showContent(boolean show) {
        listShown = show;
        controller.setListShown(show);
        progressView.setVisibility(listShown ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int trackPosition = position - listView.getHeaderViewsCount();
        playTracksAtPosition(trackPosition, expandPlayerSubscriberProvider.get());
    }

    private void playTracksAtPosition(int trackPosition, Subscriber<PlaybackResult> playbackSubscriber) {
        final PlaySessionSource playSessionSource = getPlaySessionSource();
        final TrackItem initialTrack = controller.getAdapter().getItem(trackPosition);

        PromotedSourceInfo promotedSourceInfo = getPromotedSourceInfo();
        SearchQuerySourceInfo searchQuerySourceInfo = getSearchQuerySourceInfo();

        if (promotedSourceInfo != null) {
            playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
        } else if (searchQuerySourceInfo != null) {
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        }

        if (shouldShowUpsell(initialTrack)) {
            navigator.openUpgrade(getActivity());
        } else {
            offlinePlaybackOperations
                    .playPlaylist(playlistWithTracks.getUrn(), initialTrack.getEntityUrn(), trackPosition, playSessionSource)
                    .subscribe(playbackSubscriber);
        }
    }

    private boolean shouldShowUpsell(TrackItem item) {
        return item.isMidTier() && featureOperations.upsellMidTier();
    }

    private PlaySessionSource getPlaySessionSource() {
        final String originScreen = Screen.fromBundle(getArguments()).get();
        final PlaySessionSource playlistSessionSource = PlaySessionSource.forPlaylist(originScreen, playlistWithTracks.getUrn(), playlistWithTracks.getCreatorUrn(), playlistWithTracks.getTrackCount());
        playlistSessionSource.setPromotedSourceInfo(getPromotedSourceInfo());
        return playlistSessionSource;
    }

    @Override
    public void onPlaylistContentChanged() {
        createLoadPlaylistObservable();
        if (isAdded()) {
            subscribeToLoadObservable();
        }
    }

    protected void refreshMetaData(PlaylistWithTracks playlistWithTracks) {
        if (playlistWithTracks.isOwnedBy(accountOperations.getLoggedInUserUrn())) {
            controller.showTrackRemovalOptions(playlistWithTracks.getUrn(), this);
        }

        this.playlistWithTracks = playlistWithTracks;
        playlistPresenter.setPlaylist(playlistWithTracks);
        engagementsPresenter.setPlaylistInfo(playlistWithTracks, getPlaySessionSource());

        // don't register clicks before we have a valid playlist
        final List<TrackItem> tracks = playlistWithTracks.getTracks();
        if (!tracks.isEmpty()) {
            playToggle.setVisibility(View.VISIBLE);
            AnimUtils.runFadeInAnimationOn(playToggle);
        } else {
            playToggle.setVisibility(View.GONE);
        }

        playlistPresenter.setTextVisibility(View.VISIBLE);
        headerUsernameText.setEnabled(true);
    }

    private class PlaylistSubscriber extends DefaultSubscriber<PlaylistWithTracks> {
        @Override
        public void onNext(PlaylistWithTracks playlist) {
            Log.d(PlaylistDetailActivity.LOG_TAG, "got playlist; track count = " + playlist.getTracks().size());
            refreshMetaData(playlist);
            controller.setContent(playlist, getPromotedSourceInfo());
            showContent(true);

            if (playOnLoad && controller.hasTracks()) {
                playFromBeginning();
                playOnLoad = false;
            }
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            if (e instanceof PlaylistMissingException) {
                // we successfully synced and failed to load, so the playlist is most likely gone or not accessible
                Toast.makeText(getActivity(), R.string.playlist_load_error, Toast.LENGTH_SHORT).show();
                getActivity().finish();
            } else {
                controller.setEmptyViewStatus(ErrorUtils.emptyViewStatusFromError(e));
                showContent(true);
                pullToRefreshController.stopRefreshing();
            }
        }

        @Override
        public void onCompleted() {
            controller.setEmptyViewStatus(EmptyView.Status.OK);
            pullToRefreshController.stopRefreshing();
        }
    }

    private final class RefreshSubscriber extends PlaylistSubscriber {
        @Override
        public void onError(Throwable e) {
            if (controller.hasContent()) {
                Toast.makeText(getActivity(), R.string.connection_list_error, Toast.LENGTH_SHORT).show();
                pullToRefreshController.stopRefreshing();
            } else {
                super.onError(e);
            }
        }
    }

    private final class ShowPlayerAfterPlaybackSubscriber extends ShowPlayerSubscriber {

        public ShowPlayerAfterPlaybackSubscriber(EventBus eventBus) {
            super(eventBus, playbackToastHelper);
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            if (e instanceof IllegalStateException) {
                playToggle.setChecked(false);
            }
        }
    }
}
