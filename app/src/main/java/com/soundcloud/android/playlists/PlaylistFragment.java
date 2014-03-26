package com.soundcloud.android.playlists;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
public class PlaylistFragment extends Fragment implements AdapterView.OnItemClickListener {

    @Inject
    PlaylistOperations mPlaylistOperations;
    @Inject
    PlaybackOperations mPlaybackOperations;
    @Inject
    PlaybackStateProvider mPlaybackStateProvider;
    @Inject
    ImageOperations mImageOperations;
    @Inject
    EngagementsController mEngagementsController;
    @Inject
    Provider<PlaylistDetailsController> mControllerProvider;

    private PlaylistDetailsController mController;

    private ListView mListView;
    private View mProgressView;

    private Observable<Playlist> mLoadPlaylist;
    private Subscription mSubscription = Subscriptions.empty();

    private PlayablePresenter mPlayablePresenter;
    private TextView mInfoHeaderText;
    private ToggleButton mPlayToggle;

    private boolean mListShown;

    // We still need to use broadcasts on this screen, since header does not fire play control events
    private final BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (isAdded() && PlaybackService.Broadcasts.META_CHANGED.equals(action)) {
                refreshNowPlayingState();
            }
        }
    };

    private final View.OnClickListener mOnPlayToggleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Playlist playlist = (Playlist) mPlayablePresenter.getPlayable();
            if (mPlaybackStateProvider.getPlayQueuePlaylistId() == playlist.getId()) {
                mPlaybackOperations.togglePlayback(getActivity());
            } else {
                mPlaybackOperations.playPlaylist(getActivity(), playlist, Screen.fromBundle(getArguments()));
            }
        }
    };

    public PlaylistFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @VisibleForTesting
    PlaylistFragment(PlaybackOperations playbackOperations, PlaylistOperations playlistOperations,
                     PlaybackStateProvider playbackStateProvider,
                     ImageOperations imageOperations,
                     EngagementsController engagementsController, Provider<PlaylistDetailsController> adapterProvider) {
        mPlaybackOperations = playbackOperations;
        mPlaylistOperations = playlistOperations;
        mPlaybackStateProvider = playbackStateProvider;
        mImageOperations = imageOperations;
        mEngagementsController = engagementsController;
        mControllerProvider = adapterProvider;
    }

    public static PlaylistFragment create(Uri playlistUri, Screen originScreen) {
        Bundle args = new Bundle();
        args.putParcelable(Playlist.EXTRA_URI, playlistUri);
        originScreen.addToBundle(args);

        PlaylistFragment fragment = new PlaylistFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoadPlaylist = mPlaylistOperations.loadPlaylist(getPlaylistId())
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

        mController = mControllerProvider.get();
        mController.onViewCreated(layout, getResources());

        mPlayablePresenter = new PlayablePresenter(getActivity());

        mProgressView = layout.findViewById(R.id.progress_container);

        mListView = (ListView) layout.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(mImageOperations.createScrollPauseListener(false, true));

        configureInfoViews(layout);
        mListView.setAdapter(mController.getAdapter());

        showContent(mListShown);

        mSubscription = mLoadPlaylist.subscribe(new PlaylistSubscriber());
    }


    @Override
    public void onResume() {
        super.onResume();
        refreshNowPlayingState();
    }

    private void refreshNowPlayingState() {
        mController.getAdapter().notifyDataSetChanged();
        mPlayToggle.setChecked(mPlaybackStateProvider.isPlaylistPlaying(getPlaylistId()));
    }

    @Override
    public void onStart() {
        super.onStart();
        mEngagementsController.startListeningForChanges();
        // Listen for playback changes, so that we can update the now-playing indicator
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlaybackService.Broadcasts.META_CHANGED);
        getActivity().registerReceiver(mPlaybackStatusListener, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mPlaybackStatusListener);
        mEngagementsController.stopListeningForChanges();
    }

    @Override
    public void onDestroyView() {
        mSubscription.unsubscribe();
        mController = null;
        super.onDestroyView();
    }

    private long getPlaylistId() {
        final Uri playlistUri = getArguments().getParcelable(Playlist.EXTRA_URI);
        return UriUtils.getLastSegmentAsLong(playlistUri);
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
        mListView.addHeaderView(headerView, null, false);
        return headerView;
    }

    private void setupPlaylistDetails(View detailsView) {
        mPlayablePresenter.setTitleView((TextView) detailsView.findViewById(R.id.title))
                .setUsernameView((TextView) detailsView.findViewById(R.id.username))
                .setArtwork((ImageView) detailsView.findViewById(R.id.artwork),
                        ImageSize.getFullImageSize(getActivity().getResources()),
                        R.drawable.placeholder_cells);

        mEngagementsController.bindView(detailsView, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.fromBundle(getArguments()).get();
            }
        });

        mPlayToggle = (ToggleButton) detailsView.findViewById(R.id.toggle_play_pause);
        mPlayToggle.setOnClickListener(mOnPlayToggleClick);
    }

    private void addInfoHeader() {
        View mInfoHeader = View.inflate(getActivity(), R.layout.playlist_header, null);
        mInfoHeaderText = (TextView) mInfoHeader.findViewById(android.R.id.text1);
        mListView.addHeaderView(mInfoHeader, null, false);
    }

    private void showContent(boolean show) {
        mListShown = show;
        mController.setListShown(show);
        mProgressView.setVisibility(mListShown ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int trackPosition = position - mListView.getHeaderViewsCount();
        final Track initialTrack = mController.getAdapter().getItem(trackPosition);
        final Playlist playlist = (Playlist) mPlayablePresenter.getPlayable();
        mPlaybackOperations.playPlaylistFromPosition(getActivity(), playlist, trackPosition, initialTrack,
                Screen.fromBundle(getArguments()));
    }

    protected void refreshMetaData(Playlist playlist) {
        mPlayablePresenter.setPlayable(playlist);
        mEngagementsController.setPlayable(playlist);

        final String trackCount = getResources().getQuantityString(
                R.plurals.number_of_sounds, playlist.getTrackCount(), playlist.getTrackCount());
        final String duration = ScTextUtils.formatTimestamp(playlist.duration);
        mInfoHeaderText.setText(getString(R.string.playlist_info_header_text, trackCount, duration));

        // don't register clicks before we have a valid playlist
        final List<Track> tracks = playlist.getTracks();
        if (tracks != null && tracks.size() > 0) {
            if (mPlayToggle.getVisibility() != View.VISIBLE) {
                mPlayToggle.setVisibility(View.VISIBLE);
                AnimUtils.runFadeInAnimationOn(getActivity(), mPlayToggle);
            }
        } else {
            mPlayToggle.setVisibility(View.GONE);
        }

    }

    private void updateTracksAdapter(Playlist playlist) {
        final ItemAdapter<Track> adapter = mController.getAdapter();
        adapter.clear();
        for (Track track : playlist.getTracks()) {
            adapter.addItem(track);
        }
        adapter.notifyDataSetChanged();
    }

    private final class PlaylistSubscriber extends DefaultSubscriber<Playlist> {
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
            showContent(true);
            mController.setEmptyViewStatus(EmptyListView.Status.ERROR);
        }

        @Override
        public void onCompleted() {
            mController.setEmptyViewStatus(EmptyListView.Status.OK);
        }
    }
}
