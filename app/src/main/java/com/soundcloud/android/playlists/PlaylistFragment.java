package com.soundcloud.android.playlists;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.PlaylistUrn;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
public class PlaylistFragment extends Fragment implements AdapterView.OnItemClickListener, OnRefreshListener {

    @Inject
    PlaylistOperations playlistOperations;
    @Inject
    PlaybackOperations playbackOperations;
    @Inject
    PlaybackStateProvider playbackStateProvider;
    @Inject
    ImageOperations imageOperations;
    @Inject
    EngagementsController engagementsController;
    @Inject
    Provider<PlaylistDetailsController> controllerProvider;
    @Inject
    PullToRefreshController pullToRefreshController;
    @Inject
    PlayQueueManager playQueueManager;

    private PlaylistDetailsController controller;

    private ListView listView;
    private View progressView;

    private Observable<Playlist> loadPlaylist;
    private Subscription subscription = Subscriptions.empty();

    private PlayablePresenter playablePresenter;
    private View headerUsernameText;
    private TextView infoHeaderText;
    private ToggleButton playToggle;

    private boolean listShown;

    // We still need to use broadcasts on this screen, since header does not fire play control events
    private final BroadcastReceiver playbackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) {
                refreshNowPlayingState();
            }
        }
    };

    private final View.OnClickListener onPlayToggleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Playlist playlist = (Playlist) playablePresenter.getPlayable();
            if (playQueueManager.isCurrentPlaylist(playlist.getId())) {
                playbackOperations.togglePlayback(getActivity());
            } else {
                playbackOperations.playPlaylist(getActivity(), playlist, Screen.fromBundle(getArguments()));
            }
        }
    };

    private final View.OnClickListener onHeaderTextClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ProfileActivity.startFromPlayable(getActivity(), playablePresenter.getPlayable());
        }
    };

    public PlaylistFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @VisibleForTesting
    PlaylistFragment(PlaybackOperations playbackOperations,
                     PlaylistOperations playlistOperations,
                     PlaybackStateProvider playbackStateProvider,
                     ImageOperations imageOperations,
                     EngagementsController engagementsController,
                     Provider<PlaylistDetailsController> adapterProvider,
                     PullToRefreshController pullToRefreshController,
                     PlayQueueManager playQueueManager) {
        this.playbackOperations = playbackOperations;
        this.playlistOperations = playlistOperations;
        this.playbackStateProvider = playbackStateProvider;
        this.imageOperations = imageOperations;
        this.engagementsController = engagementsController;
        this.controllerProvider = adapterProvider;
        this.pullToRefreshController = pullToRefreshController;
        this.playQueueManager = playQueueManager;
    }

    public static PlaylistFragment create(Bundle args) {
        PlaylistFragment fragment = new PlaylistFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPlaylist = playlistOperations.loadPlaylist(getPlaylistUrn())
                .observeOn(mainThread())
                .cache();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_fragment, container, false);
    }

    @Override
    public void onViewCreated(View layout, Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);

        controller = controllerProvider.get();
        controller.onViewCreated(layout, getResources());

        playablePresenter = new PlayablePresenter(getActivity());

        progressView = layout.findViewById(R.id.progress_container);

        listView = (ListView) layout.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true));

        configureInfoViews(layout);
        listView.setAdapter(controller.getAdapter());

        PullToRefreshLayout mPullToRefreshLayout = (PullToRefreshLayout) layout.findViewById(R.id.ptr_layout);
        pullToRefreshController.attach(getActivity(), mPullToRefreshLayout, this);

        showContent(listShown);

        subscription = loadPlaylist.subscribe(new PlaylistSubscriber());
    }

    @Override
    public void onRefreshStarted(View view) {
        subscription = playlistOperations.refreshPlaylist(getPlaylistUrn())
                .observeOn(mainThread())
                .subscribe(new RefreshSubscriber());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshNowPlayingState();
    }

    private void refreshNowPlayingState() {
        controller.getAdapter().notifyDataSetChanged();
        playToggle.setChecked(playQueueManager.isCurrentPlaylist(getPlaylistUrn().numericId) && playbackStateProvider.isSupposedToBePlaying());
    }

    @Override
    public void onStart() {
        super.onStart();
        engagementsController.startListeningForChanges();
        // Listen for playback changes, so that we can update the now-playing indicator
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlaybackService.Broadcasts.META_CHANGED);
        intentFilter.addAction(PlaybackService.Broadcasts.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(playbackStatusListener, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(playbackStatusListener);
        engagementsController.stopListeningForChanges();
    }

    @Override
    public void onDestroyView() {
        pullToRefreshController.detach();
        subscription.unsubscribe();
        controller = null;
        super.onDestroyView();
    }

    private PlaylistUrn getPlaylistUrn() {
        // if possible, use the instance to get the ID as it can change during syncing
        if (playablePresenter != null) {
            final Playlist playlist = (Playlist) playablePresenter.getPlayable();
            if (playlist != null) {
                return playlist.getUrn();
            }
        }
        return getArguments().getParcelable(Playlist.EXTRA_URN);
    }

    private void configureInfoViews(View layout) {
        View details = layout.findViewById(R.id.playlist_details);
        if (details == null) {
            details = createDetailsHeader();
        }
        setupPlaylistDetails(details);
        addInfoHeader();
    }

    private View createDetailsHeader() {
        View headerView = View.inflate(getActivity(), R.layout.playlist_details_view, null);
        listView.addHeaderView(headerView, null, false);
        return headerView;
    }

    private void setupPlaylistDetails(View detailsView) {
        playablePresenter.setTitleView((TextView) detailsView.findViewById(R.id.title))
                .setUsernameView((TextView) detailsView.findViewById(R.id.username))
                .setArtwork((ImageView) detailsView.findViewById(R.id.artwork),
                        ImageSize.getFullImageSize(getActivity().getResources()));

        engagementsController.bindView(detailsView, new OriginProvider() {
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

    private void addInfoHeader() {
        View mInfoHeader = View.inflate(getActivity(), R.layout.playlist_header, null);
        infoHeaderText = (TextView) mInfoHeader.findViewById(android.R.id.text1);
        listView.addHeaderView(mInfoHeader, null, false);
    }

    private void showContent(boolean show) {
        listShown = show;
        controller.setListShown(show);
        progressView.setVisibility(listShown ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int trackPosition = position - listView.getHeaderViewsCount();
        final Track initialTrack = controller.getAdapter().getItem(trackPosition);
        final Playlist playlist = (Playlist) playablePresenter.getPlayable();
        playbackOperations.playPlaylistFromPosition(getActivity(), playlist, trackPosition, initialTrack,
                Screen.fromBundle(getArguments()));
    }

    protected void refreshMetaData(Playlist playlist) {
        playablePresenter.setPlayable(playlist);
        engagementsController.setPlayable(playlist);
        infoHeaderText.setText(createHeaderText(playlist));

        // don't register clicks before we have a valid playlist
        final List<Track> tracks = playlist.getTracks();
        if (tracks != null && tracks.size() > 0) {
            if (playToggle.getVisibility() != View.VISIBLE) {
                playToggle.setVisibility(View.VISIBLE);
                AnimUtils.runFadeInAnimationOn(getActivity(), playToggle);
            }
        } else {
            playToggle.setVisibility(View.GONE);
        }

        playablePresenter.setTextVisibility(View.VISIBLE);
        headerUsernameText.setEnabled(true);
    }

    private String createHeaderText(Playlist playlist) {
        final String trackCount = getResources().getQuantityString(
                R.plurals.number_of_sounds, playlist.getTrackCount(), playlist.getTrackCount());
        final String duration = ScTextUtils.formatTimestamp(playlist.duration);
        return getString(R.string.playlist_info_header_text, trackCount, duration);
    }

    private void updateTracksAdapter(Playlist playlist) {
        final ItemAdapter<Track> adapter = controller.getAdapter();
        adapter.clear();
        for (Track track : playlist.getTracks()) {
            adapter.addItem(track);
        }
        adapter.notifyDataSetChanged();
    }

    private class PlaylistSubscriber extends DefaultSubscriber<Playlist> {
        @Override
        public void onNext(Playlist playlist) {
            Log.d(PlaylistDetailActivity.LOG_TAG, "got playlist; track count = " + playlist.getTracks().size());
            refreshMetaData(playlist);
            updateTracksAdapter(playlist);
            showContent(true);
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            controller.setEmptyViewStatus(EmptyView.Status.ERROR);
            showContent(true);
            pullToRefreshController.stopRefreshing();
        }

        @Override
        public void onCompleted() {
            controller.setEmptyViewStatus(EmptyView.Status.OK);
            pullToRefreshController.stopRefreshing();
        }
    }

    private class RefreshSubscriber extends PlaylistSubscriber {
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

}
