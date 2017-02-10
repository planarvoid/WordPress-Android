package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.transform;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.CollectionRenderer;
import com.soundcloud.android.view.adapters.CollectionViewState;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewPlaylistDetailFragment extends LightCycleSupportFragment<NewPlaylistDetailFragment>
        implements TrackItemMenuPresenter.RemoveTrackListener, NewPlaylistDetailsAdapter.PlaylistDetailView {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";
    public static final String EXTRA_AUTOPLAY = "autoplay";

    @Inject NewPlaylistDetailsPresenterFactory playlistPresenterFactory;
    @Inject PlaylistEngagementsRenderer playlistEngagementsRenderer;
    @Inject PlaylistCoverRenderer playlistCoverRenderer;
    @Inject PlaylistTrackItemRendererFactory trackItemRendererFactory;
    @Inject PlaylistEditionItemTouchCallbackFactory touchCallbackFactory;
    @Inject PlaylistDetailToolbarViewFactory toolbarViewFactory;
    @Inject NewPlaylistDetailsAdapterFactory newPlaylistDetailsAdapterFactory;
    @Inject Navigator navigator;
    @Inject BaseLayoutHelper baseLayoutHelper;

    @Inject @LightCycle NewPlaylistDetailHeaderScrollHelper headerScrollHelper;

    @Nullable private ItemTouchHelper itemTouchHelper;
    @Nullable private PlaylistDetailToolbarView toolbarView;

    private NewPlaylistDetailsPresenter presenter;
    private NewPlaylistDetailsAdapter adapter;

    private boolean skipModelUpdates;
    private View view;
    private CompositeSubscription subscription;

    private CollectionRenderer<PlaylistDetailItem, RecyclerView.ViewHolder> collectionRenderer;

    public static Fragment create(Urn playlistUrn, Screen screen, SearchQuerySourceInfo searchInfo,
                                  PromotedSourceInfo promotedInfo, boolean autoplay) {
        final NewPlaylistDetailFragment fragment = new NewPlaylistDetailFragment();
        fragment.setArguments(createBundle(playlistUrn, screen, searchInfo, promotedInfo, autoplay));
        return fragment;
    }

    private static Bundle createBundle(Urn playlistUrn,
                                       Screen screen,
                                       SearchQuerySourceInfo searchInfo,
                                       PromotedSourceInfo promotedInfo,
                                       boolean autoplay) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_URN, playlistUrn);
        bundle.putParcelable(EXTRA_QUERY_SOURCE_INFO, searchInfo);
        bundle.putParcelable(EXTRA_PROMOTED_SOURCE_INFO, promotedInfo);
        bundle.putBoolean(EXTRA_AUTOPLAY, autoplay);
        screen.addToBundle(bundle);
        return bundle;
    }

    public NewPlaylistDetailFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
        this.skipModelUpdates = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = playlistPresenterFactory.create(getArguments().getParcelable(EXTRA_URN),
                                                    Screen.fromBundle(getArguments()).get(),
                                                    getArguments().getParcelable(EXTRA_QUERY_SOURCE_INFO),
                                                    getArguments().getParcelable(EXTRA_PROMOTED_SOURCE_INFO));
        presenter.connect();

        setHasOptionsMenu(true);

        adapter = newPlaylistDetailsAdapterFactory.create(this);

        collectionRenderer = new CollectionRenderer<>(adapter, this::isTheSameItem, Object::equals, getEmptyStateProvider(), false);
    }

    private CollectionRenderer.EmptyStateProvider getEmptyStateProvider() {
        return new CollectionRenderer.EmptyStateProvider(){
            @Override
            public int waitingView() {
                return R.layout.emptyview_loading_tracks;
            }

            @Override
            public int connectionErrorView() {
                return R.layout.emptyview_connection_error;
            }

            @Override
            public int serverErrorView() {
                return R.layout.emptyview_server_error;
            }

            @Override
            public int emptyView() {
                return R.layout.emptyview_playlist_no_tracks;
            }
        };
    }

    private boolean isTheSameItem(PlaylistDetailItem item1, PlaylistDetailItem item2) {
        return isPlaylistDetailTrackItem(item1) && isPlaylistDetailTrackItem(item2)
                && trackItem(item1).getUrn().equals((trackItem(item2)).getUrn());
    }

    private PlaylistDetailTrackItem trackItem(PlaylistDetailItem newItem) {
        return (PlaylistDetailTrackItem) newItem;
    }

    private boolean isPlaylistDetailTrackItem(PlaylistDetailItem newItem) {
        return newItem instanceof PlaylistDetailTrackItem;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.new_playlist_details_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        this.itemTouchHelper = new ItemTouchHelper(touchCallbackFactory.create(this));

        boolean shouldRenderEmptyViewsAtTop = view.findViewById(R.id.appbar) != null;
        collectionRenderer.attach(view, shouldRenderEmptyViewsAtTop);

        baseLayoutHelper.setupActionBar(((AppCompatActivity) getActivity()));

        toolbarView = toolbarViewFactory.create(presenter, actionBar());

        subscription = new CompositeSubscription();
        subscription.addAll(

                presenter.viewModel()
                        .observeOn(AndroidSchedulers.mainThread())
                         .filter(ignored -> !skipModelUpdates)
                        .doOnNext(this::bindViews)
                        .map(this::convertLegacyModel)
                        .subscribe(collectionRenderer::render),

                collectionRenderer.onRefresh()
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(aVoid -> {
                    presenter.refresh();
                }),

                presenter.goToCreator()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(urn -> {
                    navigator.legacyOpenProfile(getActivity(), urn);
                }),

                presenter.goToUpsell()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(urn -> {
                    navigator.openUpgrade(getContext());
                }),

                presenter.onRepostResult()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(new RepostResultSubscriber(view.getContext())),

                presenter.onRequestingPlaylistDeletion()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(urn -> {
                             DeletePlaylistDialogFragment.show(getFragmentManager(), urn);
                         }),

                presenter.onGoBack()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(ignored -> {
                             getActivity().onBackPressed();
                         })
        );
    }


    private ActionBar actionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.playlist_details_edit_actions, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        toolbarView.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.edit_validate) {
            presenter.onExitEditMode();
            return true;
        }
        return false;
    }

    private RecyclerView recyclerView() {
        return ButterKnife.findById(getActivity(), R.id.ak_recycler_view);
    }

    private CollectionViewState<PlaylistDetailItem> convertLegacyModel(AsyncViewModel<PlaylistDetailsViewModel> playlistDetailsViewModelAsyncViewModel) {
        Optional<PlaylistDetailsViewModel> items = playlistDetailsViewModelAsyncViewModel.data();

        return CollectionViewState.<PlaylistDetailItem>builder()
                .nextPageError(playlistDetailsViewModelAsyncViewModel.error())
                .isRefreshing(playlistDetailsViewModelAsyncViewModel.isRefreshing())
                .isLoadingNextPage(!items.isPresent() || items.get().waitingForTracks())
                .hasMorePages(false)
                .items(items.isPresent() ? items.get().itemsWithoutHeader() : Collections.emptyList())
                .build();
    }

    private void bindViews(AsyncViewModel<PlaylistDetailsViewModel> asyncViewModel) {
        if (asyncViewModel.data().isPresent()) {
            PlaylistDetailsViewModel data = asyncViewModel.data().get();
            headerScrollHelper.setIsEditing(data.metadata().isInEditMode());
            bindCover(data);
            bindEngagementBar(data);
            bindToolBar(data);
            bindEditMode(data.metadata().isInEditMode());
        }

    }

    private void bindEditMode(boolean isInEditMode) {
        recyclerView().setNestedScrollingEnabled(!isInEditMode);
        headerScrollHelper.setExpanded(!isInEditMode);
        if (isInEditMode) {
            itemTouchHelper.attachToRecyclerView(recyclerView());
        } else {
            itemTouchHelper.attachToRecyclerView(null);
        }
    }

    private void bindToolBar(PlaylistDetailsViewModel data) {
        toolbarView.setPlaylist(data.metadata());
    }

    private void bindEngagementBar(PlaylistDetailsViewModel data) {
        playlistEngagementsRenderer.bind(view, data, presenter);
    }

    private void bindCover(PlaylistDetailsViewModel data) {
        playlistCoverRenderer.bind(view, data.metadata(), presenter::onHeaderPlayButtonClicked, presenter::onCreatorClicked);
    }

    @Override
    public void onDestroyView() {
        itemTouchHelper = null;
        view = null;
        toolbarView = null;
        collectionRenderer.detach();
        subscription.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        presenter.disconnect();
        super.onDestroy();
    }

    @Override
    public void onItemClicked(PlaylistDetailTrackItem trackItem) {
        presenter.onItemTriggered(trackItem);
    }

    @Override
    public void onHandleTouched(RecyclerView.ViewHolder holder) {
        itemTouchHelper.startDrag(holder);
    }

    @Override
    public void onUpsellItemDismissed(PlaylistDetailUpsellItem item) {
        presenter.onItemDismissed(item);

        final int position = adapter.getItems().indexOf(item);
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    @Override
    public void onUpsellItemClicked(PlaylistDetailUpsellItem item) {
        presenter.onItemTriggered(item);
    }

    @Override
    public void onUpsellItemPresented() {
        presenter.fireUpsellImpression();
    }

    public void onDragStarted() {
        skipModelUpdates = true;
    }

    public void dragItem(int fromPosition, int toPosition) {
        Collections.swap(adapter.getItems(), fromPosition, toPosition);
        adapter.notifyItemMoved(fromPosition, toPosition);
    }

    public void onDragStopped() {
        skipModelUpdates = false;
        saveUpdates();
    }

    @Override
    public void onPlaylistTrackRemoved(Urn trackUrn) {
        final List<Urn> urns = transform(trackItems(), PlaylistDetailTrackItem::getUrn);
        removeTrackAtPosition(urns.indexOf(trackUrn));
    }

    public void removeItem(int position) {
        removeTrackAtPosition(position);
    }

    private void removeTrackAtPosition(int trackPosition) {
        adapter.getItems().remove(trackPosition);
        adapter.notifyItemRemoved(trackPosition);
        presenter.actionUpdateTrackListWithUndo(trackItems());
    }

    private void saveUpdates() {
        presenter.actionUpdateTrackList(trackItems());
    }

    private List<PlaylistDetailTrackItem> trackItems() {
        final List<PlaylistDetailItem> items = adapter.getItems();
        final List<PlaylistDetailTrackItem> tracks = new ArrayList<>(items.size());
        for (PlaylistDetailItem item : items) {
            if (item.isTrackItem()) {
                tracks.add(((PlaylistDetailTrackItem) item));
            }
        }
        return tracks;
    }
}
