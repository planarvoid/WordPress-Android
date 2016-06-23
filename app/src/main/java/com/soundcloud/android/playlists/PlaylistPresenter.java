package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_PROMOTED_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_QUERY_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_URN;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.android.view.dragdrop.OnStartDragListener;
import com.soundcloud.android.view.dragdrop.SimpleItemTouchHelperCallback;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class PlaylistPresenter extends RecyclerViewPresenter<PlaylistWithTracks, TrackItem> implements OnStartDragListener {

    private final Func1<EntityStateChangedEvent, Boolean> IS_CURRENT_PLAYLIST_DELETED = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getKind() == EntityStateChangedEvent.ENTITY_DELETED
                    && event.getFirstUrn().equals(getPlaylistUrn());
        }
    };

    private final Func1<EntityStateChangedEvent, Boolean> IS_PLAYLIST_PUSHED_FILTER = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getKind() == EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER
                    && event.getFirstUrn().equals(getPlaylistUrn());
        }
    };

    private final Action1<PlaylistWithTracks> clearAdapter = new Action1<PlaylistWithTracks>() {
        @Override
        public void call(PlaylistWithTracks playlistWithTracks) {
            adapter.clear();
        }
    };

    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaylistOperations playlistOperations;
    private final EventBus eventBus;
    private final PlaylistAdapter adapter;
    private static final Func1<PlaylistWithTracks, Iterable<TrackItem>> TO_LIST_ITEMS = new Func1<PlaylistWithTracks, Iterable<TrackItem>>() {
        @Override
        public Iterable<TrackItem> call(PlaylistWithTracks playlistWithTracks) {
            return new ArrayList<TrackItem>(playlistWithTracks.getTracks());
        }
    };

    @LightCycle final PlaylistHeaderPresenter headerPresenter;
    @LightCycle final PlaylistContentPresenter playlistContentPresenter;

    private PlaySessionSource playSessionSource;
    private Fragment fragment;
    private Subscription subscription = RxUtils.invalidSubscription();
    private String screen;
    private ItemTouchHelper itemTouchHelper;
    private Optional<PlaylistWithTracks> playlistWithTracks;

    @Inject
    public PlaylistPresenter(PlaylistOperations playlistOperations,
                             SwipeRefreshAttacher swipeRefreshAttacher,
                             PlaylistHeaderPresenter headerPresenter,
                             PlaylistContentPresenter playlistContentPresenter,
                             PlaylistAdapterFactory adapter,
                             PlaybackInitiator playbackInitiator,
                             Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                             EventBus eventBus) {
        super(swipeRefreshAttacher);
        this.playlistOperations = playlistOperations;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.headerPresenter = headerPresenter;
        this.playlistContentPresenter = playlistContentPresenter;
        this.adapter = adapter.create(this);
        headerPresenter.setPlaylistPresenter(this);
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        this.fragment = fragment;
        getBinding().connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(getRecyclerView());
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        subscription = new CompositeSubscription(
                eventBus
                        .queue(ENTITY_STATE_CHANGED)
                        .filter(IS_CURRENT_PLAYLIST_DELETED)
                        .subscribe(new GoBackSubscriber()),
                eventBus
                        .queue(ENTITY_STATE_CHANGED)
                        .filter(IS_PLAYLIST_PUSHED_FILTER)
                        .subscribe(new PlaylistPushedSubscriber()),
                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayingTrackSubscriber(adapter)),
                eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                eventBus.subscribe(OFFLINE_CONTENT_CHANGED, new UpdateCurrentDownloadSubscriber(adapter))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        subscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        itemTouchHelper.startDrag(viewHolder);
    }

    void showEditMode() {
        fragment.getActivity().startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                if (mode != null) {
                    TextView title = (TextView) LayoutInflater.from(fragment.getContext()).inflate(R.layout.toolbar_title, null);
                    title.setText(R.string.edit_playlist);
                    mode.setCustomView(title);
                }
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                setEditMode(false);
            }
        });
    }

    void setEditMode(boolean isEditMode) {
        playlistContentPresenter.setEditMode(this, isEditMode);
        headerPresenter.setEditMode(isEditMode);
        adapter.setEditMode(isEditMode);
    }

    void savePlaylist() {
        checkState(playlistWithTracks.isPresent(), "The playlist must be loaded to be saved");

        final PlaylistWithTracks playlist = playlistWithTracks.get();
        final List<Urn> tracks = Lists.transform(adapter.getItems(), TrackItem.TO_URN);
        fireAndForget(playlistOperations.editPlaylist(playlist.getUrn(), playlist.getTitle(), playlist.isPrivate(), new ArrayList<>(tracks)));
    }

    @Override
    protected void onItemClicked(View view, int position) {
        playlistContentPresenter.onItemClicked(position);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected CollectionBinding<PlaylistWithTracks, TrackItem> onBuildBinding(Bundle fragmentArgs) {

        return CollectionBinding.from(loadPlaylistObservable(getPlaylistUrn(fragmentArgs)), TO_LIST_ITEMS)
                .withAdapter(adapter).build();
    }

    void play(int trackPosition) {
        playbackInitiator.playTracks(
                playlistOperations.trackUrnsForPlayback(playSessionSource.getCollectionUrn()),
                adapter.getItem(trackPosition).getUrn(), trackPosition, playSessionSource)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    private Observable<PlaylistWithTracks> loadPlaylistObservable(Urn playlistUrn) {
        return playlistOperations.playlist(playlistUrn).doOnNext(clearAdapter);
    }

    private Urn getPlaylistUrn(Bundle fragmentArgs) {
        return fragmentArgs.getParcelable(EXTRA_URN);
    }

    private Urn getPlaylistUrn() {
        return fragment.getArguments().getParcelable(EXTRA_URN);
    }

    private SearchQuerySourceInfo getSearchQuerySourceInfo() {
        return fragment.getArguments().getParcelable(EXTRA_QUERY_SOURCE_INFO);
    }

    @Override
    protected void onSubscribeBinding(CollectionBinding<PlaylistWithTracks, TrackItem> collectionBinding, CompositeSubscription viewLifeCycle) {
        collectionBinding.source().subscribe(new PlaylistSubscriber());
    }

    @Override
    protected CollectionBinding<PlaylistWithTracks, TrackItem> onRefreshBinding() {
        final Urn playlistUrn = getPlaylistUrn(fragment.getArguments());

        return CollectionBinding.from(playlistOperations.updatedPlaylistInfo(playlistUrn), TO_LIST_ITEMS)
                .withAdapter(adapter)
                .build();
    }

    private PromotedSourceInfo getPromotedSourceInfo() {
        return (PromotedSourceInfo) fragment.getArguments().getParcelable(EXTRA_PROMOTED_SOURCE_INFO);
    }

    void setScreen(String screen) {
        this.screen = screen;
        headerPresenter.setScreen(screen);
    }

    void reloadPlaylist() {
        retryWith(CollectionBinding
                .from(loadPlaylistObservable(getPlaylistUrn()), TO_LIST_ITEMS)
                .withAdapter(adapter).build());
    }

    private class GoBackSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent args) {
            // This actually go back in the stack because the fragment is tied up
            // to PlaylistDetailActivity.
            if (fragment.isAdded()) {
                fragment.getActivity().finish();
            }
        }
    }

    protected class PlaylistSubscriber extends DefaultSubscriber<PlaylistWithTracks> {

        @Override
        public void onNext(PlaylistWithTracks playlist) {
            playlistWithTracks = Optional.of(playlist);
            playSessionSource = createPlaySessionSource(playlist);
            headerPresenter.setPlaylist(PlaylistHeaderItem.create(playlist, playSessionSource));
            fragment.getActivity().setTitle(playlist.getPlaylistItem().getLabel(fragment.getContext()));
        }
    }

    protected PlaySessionSource createPlaySessionSource(PlaylistWithTracks playlistWithTracks) {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(
                screen,
                playlistWithTracks.getUrn(),
                playlistWithTracks.getCreatorUrn(),
                playlistWithTracks.getTrackCount());

        PromotedSourceInfo promotedSourceInfo = getPromotedSourceInfo();
        SearchQuerySourceInfo searchQuerySourceInfo = getSearchQuerySourceInfo();

        if (promotedSourceInfo != null) {
            playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
        } else if (searchQuerySourceInfo != null) {
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        }
        return playSessionSource;
    }

    private class PlaylistPushedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent args) {
            final PropertySet updatedPlaylist = args.getNextChangeSet();
            fragment.getArguments().putParcelable(EXTRA_URN, updatedPlaylist.get(PlaylistProperty.URN));
        }
    }

}
