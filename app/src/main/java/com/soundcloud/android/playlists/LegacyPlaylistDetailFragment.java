package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.playlists.PlaylistOperations.PlaylistMissingException;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
@SuppressWarnings("PMD.TooManyFields")
public class LegacyPlaylistDetailFragment extends LightCycleSupportFragment<LegacyPlaylistDetailFragment> implements
        AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener,
        PlaylistDetailsController.Listener {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";
    public static final String EXTRA_AUTOPLAY = "autoplay";

    private final Func1<EntityStateChangedEvent, Boolean> IS_CURRENT_PLAYLIST_DELETED =
            new Func1<EntityStateChangedEvent, Boolean>() {
                @Override
                public Boolean call(EntityStateChangedEvent event) {
                    return event.getKind() == EntityStateChangedEvent.ENTITY_DELETED
                            && event.getFirstUrn().equals(getPlaylistUrn());
                }
            };

    private final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_PUSHED_FILTER =
            new Func1<EntityStateChangedEvent, Boolean>() {
                @Override
                public Boolean call(EntityStateChangedEvent event) {
                    return event.getKind() == EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER
                            && event.getFirstUrn().equals(getPlaylistUrn());
                }
            };

    @Inject PlaylistDetailsController.Provider controllerProvider;
    @Inject PlaylistOperations playlistOperations;
    @Inject PlaySessionController playSessionController;
    @Inject PlaybackInitiator playbackInitiator;
    @Inject ImageOperations imageOperations;
    @Inject @LightCycle LegacyPlaylistEngagementsPresenter engagementsPresenter;
    @Inject @LightCycle PullToRefreshController pullToRefreshController;
    @Inject PlayQueueManager playQueueManager;
    @Inject EventBus eventBus;
    @Inject PlaylistHeaderViewFactory playlistHeaderViewFactory;
    @Inject Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    @Inject AccountOperations accountOperations;
    @Inject Navigator navigator;

    private PlaylistDetailsController controller;

    private ListView listView;
    private View progressView;

    private Observable<PlaylistWithTracks> loadPlaylist;
    private Subscription playlistSubscription = RxUtils.invalidSubscription();
    private CompositeSubscription eventSubscription = new CompositeSubscription();

    private PlaylistHeaderView playlistHeaderView;
    private View headerUsernameText;
    private ImageButton playToggle;
    private PlaylistWithTracks playlistWithTracks;

    private boolean listShown;
    private boolean playOnLoad;

    private final View.OnClickListener onPlayClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playFromBeginning();
        }
    };

    private final View.OnClickListener onHeaderTextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.legacyOpenProfile(getActivity(), playlistWithTracks.getCreatorUrn());
        }
    };

    public static LegacyPlaylistDetailFragment create(Urn playlistUrn, Screen screen, SearchQuerySourceInfo searchInfo,
                                                      PromotedSourceInfo promotedInfo, boolean autoplay) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_URN, playlistUrn);
        bundle.putParcelable(EXTRA_QUERY_SOURCE_INFO, searchInfo);
        bundle.putParcelable(EXTRA_PROMOTED_SOURCE_INFO, promotedInfo);
        bundle.putBoolean(EXTRA_AUTOPLAY, autoplay);
        screen.addToBundle(bundle);
        LegacyPlaylistDetailFragment fragment = new LegacyPlaylistDetailFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public LegacyPlaylistDetailFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    LegacyPlaylistDetailFragment(PlaylistDetailsController.Provider controllerProvider,
                                 PlaySessionController playSessionController,
                                 PlaybackInitiator playbackInitiator,
                                 PlaylistOperations playlistOperations,
                                 EventBus eventBus,
                                 ImageOperations imageOperations,
                                 LegacyPlaylistEngagementsPresenter engagementsPresenter,
                                 PullToRefreshController pullToRefreshController,
                                 PlayQueueManager playQueueManager,
                                 PlaylistHeaderViewFactory playlistHeaderViewFactory,
                                 Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                 AccountOperations accountOperations,
                                 Navigator navigator) {
        this.controllerProvider = controllerProvider;
        this.playSessionController = playSessionController;
        this.playlistOperations = playlistOperations;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;
        this.imageOperations = imageOperations;
        this.engagementsPresenter = engagementsPresenter;
        this.pullToRefreshController = pullToRefreshController;
        this.playQueueManager = playQueueManager;
        this.playlistHeaderViewFactory = playlistHeaderViewFactory;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.accountOperations = accountOperations;
        this.navigator = navigator;
        addLifeCycleComponents();
    }

    private void playFromBeginning() {
        final TrackItem first = (TrackItem) controller.getAdapter().getItem(0);
        playTracksAtPosition(first, 0, expandPlayerSubscriberProvider.get());
    }

    private void addLifeCycleComponents() {
        pullToRefreshController.setRefreshListener(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLoadPlaylistObservable();
        if (savedInstanceState == null) {
            playOnLoad = getArguments().getBoolean(EXTRA_AUTOPLAY, false);
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
        loadPlaylist = playlistOperations.playlist(getPlaylistUrn()).observeOn(mainThread());
    }

    private void subscribeToLoadObservable() {
        playlistSubscription.unsubscribe();
        playlistSubscription = loadPlaylist.subscribe(new PlaylistSubscriber());
    }

    @Override
    public void onRefresh() {
        playlistSubscription.unsubscribe();
        playlistSubscription = playlistOperations.updatedPlaylistInfo(getPlaylistUrn())
                                                 .observeOn(mainThread())
                                                 .subscribe(new RefreshSubscriber());
    }

    @Override
    public void onResume() {
        super.onResume();
        eventSubscription.add(
                eventBus.queue(EventQueue.ENTITY_STATE_CHANGED)
                        .filter(EntityStateChangedEvent.IS_TRACK_ADDED_TO_PLAYLIST_FILTER)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new PlaylistContentChangedSubscriber()));

        eventSubscription.add(eventBus
                                      .queue(ENTITY_STATE_CHANGED)
                                      .filter(IS_CURRENT_PLAYLIST_DELETED)
                                      .subscribe(new GoBackSubscriber()));

        eventSubscription.add(eventBus
                                      .queue(ENTITY_STATE_CHANGED)
                                      .filter(IS_PLAYLIST_PUSHED_FILTER)
                                      .subscribe(new PlaylistPushedSubscriber()));
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
        playlistHeaderView = playlistHeaderViewFactory.create(detailsView);
        playlistHeaderView.setOnPlayButtonClickListener(onPlayClick);
        playlistHeaderView.setOnCreatorButtonClickListener(onHeaderTextClick);
        engagementsPresenter.bindView(detailsView, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.fromBundle(getArguments()).get();
            }
        });

        playToggle = (ImageButton) detailsView.findViewById(R.id.btn_play);

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
        // Ignore clicks for upsell items
        final TypedListItem item = controller.getAdapter().getItem(trackPosition);
        if (item instanceof TrackItem) {
            playTracksAtPosition((TrackItem) item, trackPosition, expandPlayerSubscriberProvider.get());
        }
    }

    private void playTracksAtPosition(TrackItem initialTrack, int position, Subscriber<PlaybackResult> playbackSubscriber) {
        final PlaySessionSource playSessionSource = getPlaySessionSource();

        PromotedSourceInfo promotedSourceInfo = getPromotedSourceInfo();
        SearchQuerySourceInfo searchQuerySourceInfo = getSearchQuerySourceInfo();

        if (promotedSourceInfo != null) {
            playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
        } else if (searchQuerySourceInfo != null) {
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        }

        playbackInitiator.playTracks(playlistOperations.trackUrnsForPlayback(playlistWithTracks.getUrn()),
                                     initialTrack.getUrn(), position, playSessionSource)
                         .subscribe(playbackSubscriber);
    }

    private PlaySessionSource getPlaySessionSource() {
        final String originScreen = Screen.fromBundle(getArguments()).get();
        final PlaySessionSource playlistSessionSource = PlaySessionSource.forPlaylist(originScreen,
                                                                                      playlistWithTracks.getUrn(),
                                                                                      playlistWithTracks.getCreatorUrn(),
                                                                                      playlistWithTracks.getTrackCount());
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
            controller.showTrackRemovalOptions(this);
        }

        this.playlistWithTracks = playlistWithTracks;
        PlaylistItem playlistItem = playlistWithTracks.getPlaylistItem();
        getActivity().setTitle(playlistItem.getLabel(getContext()));
        playlistHeaderView.setPlaylist(playlistItem, !playlistWithTracks.getTracks().isEmpty());
        engagementsPresenter.setPlaylistInfo(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));

        // don't register clicks before we have a valid playlist
        final List<TrackItem> tracks = playlistWithTracks.getTracks();
        if (!tracks.isEmpty()) {
            playToggle.setVisibility(View.VISIBLE);
            AnimUtils.runFadeInAnimationOn(playToggle);
        } else {
            playToggle.setVisibility(View.GONE);
        }
        headerUsernameText.setEnabled(true);
    }

    private class PlaylistSubscriber extends DefaultSubscriber<PlaylistWithTracks> {
        @Override
        public void onNext(PlaylistWithTracks playlist) {
            Log.d(PlaylistDetailActivity.LOG_TAG, "got playlist; track count = " + playlist.getTracks().size());
            refreshMetaData(playlist);
            controller.setContent(playlist, getPromotedSourceInfo());
            setEmptyStateMessage(playlist.getPlaylistItem());

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

        private void setEmptyStateMessage(PlaylistItem playlistItem) {
            final String label = getContext().getString(PlaylistItem.getSetTypeLabel(playlistItem.getPlayableType()));
            final String message = getContext().getString(R.string.custom_empty_playlist_title, label);
            final String secondaryText = getContext().getString(R.string.custom_empty_playlist_description, label);
            controller.setEmptyStateMessage(message, secondaryText);
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

    private class GoBackSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent args) {
            // This actually go back in the stack because the fragment is tied up
            // to PlaylistDetailActivity.
            if (isAdded()) {
                getActivity().finish();
            }
        }
    }

    private class PlaylistPushedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent args) {
            final PropertySet updatedPlaylist = args.getNextChangeSet();
            playlistWithTracks.update(updatedPlaylist);
            getArguments().putParcelable(EXTRA_URN, updatedPlaylist.get(PlaylistProperty.URN));
        }
    }

    private class PlaylistContentChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            if (event.getFirstUrn().equals(playlistWithTracks.getUrn())) {
                onPlaylistContentChanged();
            }
        }
    }
}
