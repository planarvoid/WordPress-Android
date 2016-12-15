package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_PROMOTED_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_QUERY_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_URN;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
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
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
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

import android.content.Context;
import android.content.res.Resources;
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

class PlaylistPresenter extends RecyclerViewPresenter<PlaylistWithTracks, PlaylistDetailItem> implements OnStartDragListener, PlaylistUpsellItemRenderer.Listener {

    private final Func1<EntityStateChangedEvent, Boolean> isCurrentPlaylistDeleted = event -> event.getKind() == EntityStateChangedEvent.ENTITY_DELETED
            && event.getFirstUrn().equals(getPlaylistUrn());

    private final Func1<EntityStateChangedEvent, Boolean> isCurrentPlaylistPushed = event -> event.getKind() == EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER
            && event.getFirstUrn().equals(getPlaylistUrn());

    private final Action1<PlaylistWithTracks> clearAdapter = new Action1<PlaylistWithTracks>() {
        @Override
        public void call(PlaylistWithTracks playlistWithTracks) {
            adapter.clear();
        }
    };

    private final Func1<PlaylistWithTracks, Iterable<PlaylistDetailItem>> toListItems = new Func1<PlaylistWithTracks, Iterable<PlaylistDetailItem>>() {
        @Override
        public Iterable<PlaylistDetailItem> call(PlaylistWithTracks playlistWithTracks) {
            List<PlaylistDetailItem> playlistDetailItems = upsellOperations.toListItems(playlistWithTracks);
            if (addInlineHeader) {
                playlistDetailItems.add(0, new PlaylistDetailHeaderItem());
            }
            return playlistDetailItems;
        }
    };

    @LightCycle final PlaylistHeaderPresenter headerPresenter;
    @LightCycle final PlaylistContentPresenter playlistContentPresenter;

    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaylistOperations playlistOperations;
    private final PlaylistUpsellOperations upsellOperations;
    private final Navigator navigator;
    private final EventBus eventBus;
    private final PlaylistAdapter adapter;
    private final boolean addInlineHeader;

    private PlaySessionSource playSessionSource;
    private Fragment fragment;
    private Subscription subscription = RxUtils.invalidSubscription();
    private String screen;
    private ItemTouchHelper itemTouchHelper;
    private Optional<PlaylistWithTracks> playlistWithTracks;

    @Inject
    public PlaylistPresenter(PlaylistOperations playlistOperations,
                             PlaylistUpsellOperations upsellOperations,
                             SwipeRefreshAttacher swipeRefreshAttacher,
                             PlaylistHeaderPresenter headerPresenter,
                             PlaylistContentPresenter playlistContentPresenter,
                             PlaylistAdapterFactory adapterFactory,
                             PlaybackInitiator playbackInitiator,
                             Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                             Navigator navigator,
                             EventBus eventBus,
                             Resources resources) {
        super(swipeRefreshAttacher);
        this.playlistOperations = playlistOperations;
        this.upsellOperations = upsellOperations;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.headerPresenter = headerPresenter;
        this.playlistContentPresenter = playlistContentPresenter;
        this.navigator = navigator;
        this.adapter = adapterFactory.create(this, headerPresenter);
        headerPresenter.setPlaylistPresenter(this);
        addInlineHeader = !resources.getBoolean(R.bool.split_screen_details_pages);

        adapter.setOnUpsellClickListener(this);

        setEditMode(false);
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
                        .filter(isCurrentPlaylistDeleted)
                        .subscribe(new GoBackSubscriber()),
                eventBus
                        .queue(ENTITY_STATE_CHANGED)
                        .filter(isCurrentPlaylistPushed)
                        .subscribe(new PlaylistPushedSubscriber()),
                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayableAdapterSubscriber(adapter)),
                eventBus.subscribe(ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                eventBus.subscribe(LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
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
                    TextView title = (TextView) LayoutInflater.from(fragment.getContext())
                                                              .inflate(R.layout.toolbar_title, null);
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
        final List<Urn> tracks = Lists.transform(adapter.getTracks(), TrackItem.TO_URN);
        fireAndForget(playlistOperations.editPlaylist(playlist.getUrn(),
                                                      playlist.getTitle(),
                                                      playlist.isPrivate(),
                                                      new ArrayList<>(tracks)));
    }

    @Override
    protected void onItemClicked(View view, int position) {
        if (adapter.getItem(position) instanceof PlaylistDetailTrackItem) {
            playlistContentPresenter.onItemClicked(position);
        }

    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    protected CollectionBinding<PlaylistWithTracks, PlaylistDetailItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(loadPlaylistObservable(getPlaylistUrn(fragmentArgs)), toListItems)
                                .withAdapter(adapter).build();
    }

    void playFromBegninning() {
        handlItemClick(addInlineHeader ? 1 : 0);
    }

    void handlItemClick(int position) {
        playbackInitiator.playTracks(
                playlistOperations.trackUrnsForPlayback(playSessionSource.getCollectionUrn()),
                ((PlaylistDetailTrackItem) adapter.getItem(position)).getUrn(), position - (addInlineHeader ? 1 : 0), playSessionSource)
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
    protected void onSubscribeBinding(CollectionBinding<PlaylistWithTracks, PlaylistDetailItem> collectionBinding,
                                      CompositeSubscription viewLifeCycle) {
        collectionBinding.source().subscribe(new PlaylistSubscriber());
    }

    @Override
    protected CollectionBinding<PlaylistWithTracks, PlaylistDetailItem> onRefreshBinding() {
        final Urn playlistUrn = getPlaylistUrn(fragment.getArguments());

        return CollectionBinding.from(playlistOperations.updatedPlaylistInfo(playlistUrn), toListItems)
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
                          .from(loadPlaylistObservable(getPlaylistUrn()), toListItems)
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
            headerPresenter.setPlaylist(playlist, playSessionSource);
            fragment.getActivity().setTitle(playlist.getPlaylistItem().getLabel(fragment.getContext()));
        }
    }

    private PlaySessionSource createPlaySessionSource(PlaylistWithTracks playlistWithTracks) {
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

    @Override
    public void onUpsellItemDismissed(int position) {
        upsellOperations.disableUpsell();
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    @Override
    public void onUpsellItemClicked(Context context) {
        navigator.openUpgrade(context);
        if (playlistWithTracks.isPresent()) {
            eventBus.publish(EventQueue.TRACKING,
                    UpgradeFunnelEvent.forPlaylistTracksClick(playlistWithTracks.get().getUrn()));
        }
    }

    @Override
    public void onUpsellItemCreated() {
        if (playlistWithTracks.isPresent()) {
            eventBus.publish(EventQueue.TRACKING,
                    UpgradeFunnelEvent.forPlaylistTracksImpression(playlistWithTracks.get().getUrn()));
        }
    }

}
