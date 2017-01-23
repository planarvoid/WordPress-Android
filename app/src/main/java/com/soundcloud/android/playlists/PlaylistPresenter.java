package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAYLIST_CHANGED;
import static com.soundcloud.android.events.EventQueue.REPOST_CHANGED;
import static com.soundcloud.android.events.EventQueue.TRACK_CHANGED;
import static com.soundcloud.android.events.EventQueue.URN_STATE_CHANGED;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_PROMOTED_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_QUERY_SOURCE_INFO;
import static com.soundcloud.android.playlists.PlaylistDetailFragment.EXTRA_URN;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.OfflinePropertiesSubscriber;
import com.soundcloud.android.view.adapters.RepostEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateTrackListSubscriber;
import com.soundcloud.android.view.dragdrop.OnStartDragListener;
import com.soundcloud.android.view.dragdrop.SimpleItemTouchHelperCallback;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import java.util.Map;

class PlaylistPresenter extends RecyclerViewPresenter<PlaylistDetailsViewModel, PlaylistDetailItem>
        implements OnStartDragListener, PlaylistUpsellItemRenderer.Listener, TrackItemMenuPresenter.RemoveTrackListener {

    @LightCycle final PlaylistHeaderPresenter headerPresenter;
    @LightCycle final PlaylistContentPresenter playlistContentPresenter;

    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaylistOperations playlistOperations;
    private final PlaylistUpsellOperations upsellOperations;
    private final Navigator navigator;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final FeatureFlags featureFlags;
    private final EventBus eventBus;
    private final PlaylistAdapter adapter;
    private final boolean addInlineHeader;
    private final PlaylistTrackItemRenderer trackRenderer;

    private PlaySessionSource playSessionSource;
    private Fragment fragment;
    private Subscription subscription = RxUtils.invalidSubscription();
    private String screen;
    private ItemTouchHelper itemTouchHelper;
    private Optional<PlaylistDetailHeaderItem> item;

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
                             Resources resources,
                             PlaylistTrackItemRendererFactory trackRendererFactory,
                             OfflinePropertiesProvider offlinePropertiesProvider,
                             FeatureFlags featureFlags) {
        super(swipeRefreshAttacher);
        this.playlistOperations = playlistOperations;
        this.upsellOperations = upsellOperations;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.headerPresenter = headerPresenter;
        this.playlistContentPresenter = playlistContentPresenter;
        this.navigator = navigator;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.trackRenderer = trackRendererFactory.create(this);
        this.adapter = adapterFactory.create(this, headerPresenter, trackRenderer);
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
    public void onPlaylistTrackRemoved(Urn track) {
        final int position = findTrackPosition(track);
        checkState(position >= 0, "Track could not be found in adapter.");
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    private int findTrackPosition(Urn track) {
        return Iterables.indexOf(adapter.getItems(), item -> item instanceof PlaylistDetailTrackItem && ((PlaylistDetailTrackItem) item).getUrn().equals(track));
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
                        .queue(URN_STATE_CHANGED)
                        .filter(event -> event.kind() == UrnStateChangedEvent.Kind.ENTITY_DELETED)
                        .filter(UrnStateChangedEvent::containsPlaylist)
                        .subscribe(new GoBackSubscriber()),
                eventBus
                        .queue(PLAYLIST_CHANGED)
                        .filter(event1 -> event1.kind() == PlaylistChangedEvent.Kind.PLAYLIST_PUSHED_TO_SERVER)
                        .subscribe(new PlaylistPushedSubscriber()),
                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM, new UpdatePlayableAdapterSubscriber(adapter)),
                eventBus.subscribe(TRACK_CHANGED, new UpdateTrackListSubscriber(adapter)),
                eventBus.subscribe(LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
                eventBus.subscribe(REPOST_CHANGED, new RepostEntityListSubscriber(adapter)),
                subscribeToOfflineContent()
        );
    }

    private Subscription subscribeToOfflineContent() {
        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            return offlinePropertiesProvider.states()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new OfflinePropertiesSubscriber<>(adapter));
        } else {
            return eventBus.subscribe(OFFLINE_CONTENT_CHANGED, new UpdateCurrentDownloadSubscriber(adapter));
        }
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
        checkState(item.isPresent(), "The playlist must be loaded to be saved");

        final PlaylistDetailHeaderItem playlist = item.get();
        final List<Urn> tracks = Lists.transform(adapter.getTracks(), TrackItem.TO_URN);
        fireAndForget(playlistOperations.editPlaylist(playlist.getUrn(),
                                                      playlist.title(),
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
    protected CollectionBinding<PlaylistDetailsViewModel, PlaylistDetailItem> onBuildBinding(Bundle fragmentArgs) {
        final Urn playlistUrn = getPlaylistUrn(fragmentArgs);
        return CollectionBinding.from(loadPlaylistObservable(playlistUrn), PlaylistDetailsViewModel::itemsWithHeader)
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

    private Observable<PlaylistDetailsViewModel> loadPlaylistObservable(Urn playlistUrn) {
        return playlistOperations.playlistWithTracksAndRecommendations(playlistUrn);
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
    protected void onSubscribeBinding(CollectionBinding<PlaylistDetailsViewModel, PlaylistDetailItem> collectionBinding,
                                      CompositeSubscription viewLifeCycle) {
        viewLifeCycle.add(collectionBinding.source().subscribe(new PlaylistSubscriber()));
    }

    @Override
    protected CollectionBinding<PlaylistDetailsViewModel, PlaylistDetailItem> onRefreshBinding() {
        final Urn playlistUrn = getPlaylistUrn(fragment.getArguments());
        return CollectionBinding.from(playlistOperations.updatedPlaylistWithTracksAndRecommendations(playlistUrn),
                                      PlaylistDetailsViewModel::itemsWithHeader)
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
                          .from(loadPlaylistObservable(getPlaylistUrn()), PlaylistDetailsViewModel::itemsWithHeader)
                          .withAdapter(adapter).build());
    }

    private class GoBackSubscriber extends DefaultSubscriber<UrnStateChangedEvent> {
        @Override
        public void onNext(UrnStateChangedEvent args) {
            // This actually go back in the stack because the fragment is tied up
            // to PlaylistDetailActivity.
            if (fragment.isAdded()) {
                fragment.getActivity().finish();
            }
        }
    }

    private class PlaylistSubscriber extends DefaultSubscriber<PlaylistDetailsViewModel> {

        @Override
        public void onNext(PlaylistDetailsViewModel playlistDetailsViewModel) {
            FragmentActivity activity = fragment.getActivity();
            // Note: This subscriber might get called after the activity has been detached
            // and it should be safe to discard it
            if (activity != null) {
                final PlaylistDetailHeaderItem headerItem = playlistDetailsViewModel.header();
                PlaylistPresenter.this.item = Optional.of(headerItem);
                playSessionSource = createPlaySessionSource(headerItem);
                headerPresenter.setPlaylist(headerItem, playSessionSource);
                fragment.getActivity().setTitle(headerItem.label());
                trackRenderer.setPlaylistInformation(playSessionSource.getPromotedSourceInfo(), headerItem.getUrn(), headerItem.creatorUrn());
            }
        }
    }

    private PlaySessionSource createPlaySessionSource(PlaylistDetailHeaderItem item) {
        PlaySessionSource playSessionSource = PlaySessionSource.forPlaylist(
                screen,
                item.getUrn(),
                item.creatorUrn(),
                item.trackCount());

        PromotedSourceInfo promotedSourceInfo = getPromotedSourceInfo();
        SearchQuerySourceInfo searchQuerySourceInfo = getSearchQuerySourceInfo();

        if (promotedSourceInfo != null) {
            playSessionSource.setPromotedSourceInfo(promotedSourceInfo);
        } else if (searchQuerySourceInfo != null) {
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        }
        return playSessionSource;
    }

    private class PlaylistPushedSubscriber extends DefaultSubscriber<PlaylistChangedEvent> {
        @Override
        public void onNext(PlaylistChangedEvent args) {
            if (args.isEntityChangeEvent()) {
                final Map<Urn, Playlist> urnPlaylistItemMap = ((PlaylistEntityChangedEvent) args).changeMap();
                if (urnPlaylistItemMap.containsKey(getPlaylistUrn())) {
                    final Playlist updatedPlaylist = urnPlaylistItemMap.get(getPlaylistUrn());
                    fragment.getArguments().putParcelable(EXTRA_URN, updatedPlaylist.urn());
                }
            }
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
        if (item.isPresent()) {
            eventBus.publish(EventQueue.TRACKING,
                             UpgradeFunnelEvent.forPlaylistTracksClick(item.get().getUrn()));
        }
    }

    @Override
    public void onUpsellItemCreated() {
        if (item.isPresent()) {
            eventBus.publish(EventQueue.TRACKING,
                             UpgradeFunnelEvent.forPlaylistTracksImpression(item.get().getUrn()));
        }
    }
}
