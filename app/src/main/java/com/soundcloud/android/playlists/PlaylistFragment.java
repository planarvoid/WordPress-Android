package com.soundcloud.android.playlists;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.tracks.TrackProperty;
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
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressLint("ValidFragment")
public class PlaylistFragment extends Fragment implements AdapterView.OnItemClickListener, OnRefreshListener {

    @Inject PlaylistDetailsController controller;
    @Inject LegacyPlaylistOperations legacyPlaylistOperations;
    @Inject PlaylistOperations playlistOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject PlaybackStateProvider playbackStateProvider;
    @Inject ImageOperations imageOperations;
    @Inject EngagementsController engagementsController;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject PlayQueueManager playQueueManager;

    private ListView listView;
    private View progressView;

    private Observable<PublicApiPlaylist> loadPlaylist;
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
            final PublicApiPlaylist playlist = (PublicApiPlaylist) playablePresenter.getPlayable();
            if (playQueueManager.isCurrentPlaylist(playlist.getId())) {
                playbackOperations.togglePlayback();
            } else {
                playbackOperations.playPlaylist(playlist, Screen.fromBundle(getArguments()));
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
    PlaylistFragment(PlaylistDetailsController controller,
                     PlaybackOperations playbackOperations,
                     LegacyPlaylistOperations legacyPlaylistOperations,
                     PlaybackStateProvider playbackStateProvider,
                     ImageOperations imageOperations,
                     EngagementsController engagementsController,
                     PullToRefreshController pullToRefreshController,
                     PlayQueueManager playQueueManager) {
        this.controller = controller;
        this.playbackOperations = playbackOperations;
        this.legacyPlaylistOperations = legacyPlaylistOperations;
        this.playbackStateProvider = playbackStateProvider;
        this.imageOperations = imageOperations;
        this.engagementsController = engagementsController;
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
        loadPlaylist = legacyPlaylistOperations.loadPlaylist(getPlaylistUrn())
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

        controller.onViewCreated(layout, getResources());

        playablePresenter = new PlayablePresenter(getActivity());

        progressView = layout.findViewById(R.id.progress_container);

        listView = (ListView) layout.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true));

        configureInfoViews(layout);
        listView.setAdapter(controller.getAdapter());

        pullToRefreshController.onViewCreated(this, this);

        showContent(listShown);

        subscription = loadPlaylist.subscribe(new PlaylistSubscriber());
    }

    @Override
    public void onRefreshStarted(View view) {
        subscription = legacyPlaylistOperations.refreshPlaylist(getPlaylistUrn())
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
        pullToRefreshController.onDestroyView();
        subscription.unsubscribe();
        super.onDestroyView();
    }

    private PlaylistUrn getPlaylistUrn() {
        // if possible, use the instance to get the ID as it can change during syncing
        if (playablePresenter != null) {
            final PublicApiPlaylist playlist = (PublicApiPlaylist) playablePresenter.getPlayable();
            if (playlist != null) {
                return playlist.getUrn();
            }
        }
        return getArguments().getParcelable(PublicApiPlaylist.EXTRA_URN);
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
                        ApiImageSize.getFullImageSize(getActivity().getResources()));

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
        View infoHeader = View.inflate(getActivity(), R.layout.playlist_header, null);
        infoHeaderText = (TextView) infoHeader.findViewById(android.R.id.text1);
        listView.addHeaderView(infoHeader, null, false);
    }

    private void showContent(boolean show) {
        listShown = show;
        controller.setListShown(show);
        progressView.setVisibility(listShown ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int trackPosition = position - listView.getHeaderViewsCount();
        final PublicApiPlaylist playlist = (PublicApiPlaylist) playablePresenter.getPlayable();
        final PropertySet initialTrack = controller.getAdapter().getItem(trackPosition);

        playbackOperations.playPlaylistFromPosition(getActivity(), playlist.toPropertySet(),
                playlistOperations.trackUrnsForPlayback(playlist.getUrn()),
                initialTrack.get(TrackProperty.URN),
                trackPosition, Screen.fromBundle(getArguments()));
    }

    protected void refreshMetaData(PublicApiPlaylist playlist) {
        playablePresenter.setPlayable(playlist);
        engagementsController.setPlayable(playlist);
        infoHeaderText.setText(createHeaderText(playlist));

        // don't register clicks before we have a valid playlist
        final List<PublicApiTrack> tracks = playlist.getTracks();
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

    private String createHeaderText(PublicApiPlaylist playlist) {
        final String trackCount = getResources().getQuantityString(
                R.plurals.number_of_sounds, playlist.getTrackCount(), playlist.getTrackCount());
        final String duration = ScTextUtils.formatTimestamp(playlist.duration, TimeUnit.MILLISECONDS);
        return getString(R.string.playlist_info_header_text, trackCount, duration);
    }

    private void updateTracksAdapter(PublicApiPlaylist playlist) {
        final ItemAdapter<PropertySet> adapter = controller.getAdapter();
        adapter.clear();
        for (PublicApiTrack track : playlist.getTracks()) {
            adapter.addItem(track.toPropertySet());
        }
        adapter.notifyDataSetChanged();
    }

    private class PlaylistSubscriber extends DefaultSubscriber<PublicApiPlaylist> {
        @Override
        public void onNext(PublicApiPlaylist playlist) {
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
