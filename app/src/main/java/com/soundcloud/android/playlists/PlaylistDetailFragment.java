package com.soundcloud.android.playlists;

import static com.soundcloud.java.collections.Lists.transform;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.CollectionLoadingState;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.settings.OfflineStorageErrorDialog;
import com.soundcloud.android.share.SharePresenter;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.DefaultEmptyStateProvider;
import com.soundcloud.android.view.EmptyStatus;
import com.soundcloud.android.view.SmoothLinearLayoutManager;
import com.soundcloud.android.view.collection.CollectionRenderer;
import com.soundcloud.android.view.collection.CollectionRendererState;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistDetailFragment extends LightCycleSupportFragment<PlaylistDetailFragment>
        implements TrackItemMenuPresenter.RemoveTrackListener, PlaylistDetailsAdapter.PlaylistDetailView {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";
    public static final String EXTRA_AUTOPLAY = "autoplay";

    @Inject PlaylistDetailsPresenterFactory playlistPresenterFactory;
    @Inject PlaylistEngagementsRenderer playlistEngagementsRenderer;
    @Inject PlaylistCoverRenderer playlistCoverRenderer;
    @Inject PlaylistTrackItemRendererFactory trackItemRendererFactory;
    @Inject PlaylistEditionItemTouchCallbackFactory touchCallbackFactory;
    @Inject PlaylistDetailsAdapterFactory newPlaylistDetailsAdapterFactory;
    @Inject Navigator navigator;
    @Inject SharePresenter shareOperations;
    @Inject PlaylistDetailsHeaderRendererFactory playlistDetailsHeaderRendererFactory;
    @Inject PlaylistDetailsHeaderAnimatorFactory headerAnimatorFactory;
    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject @LightCycle PlaylistDetailToolbarView toolbarView;
    @Inject @LightCycle PlaylistDetailHeaderScrollHelper headerScrollHelper;

    @Nullable private ItemTouchHelper itemTouchHelper;
    @Nullable private PlaylistDetailsHeaderAnimator headerAnimator;

    private PlaylistDetailsPresenter presenter;
    private PlaylistDetailsAdapter adapter;

    private boolean skipModelUpdates;
    private CompositeSubscription subscription;

    private CollectionRenderer<PlaylistDetailItem, RecyclerView.ViewHolder> collectionRenderer;
    private PlaylistDetailsInputs inputs;

    public static Fragment create(Urn playlistUrn, Screen screen, SearchQuerySourceInfo searchInfo,
                                  PromotedSourceInfo promotedInfo, boolean autoplay) {
        final PlaylistDetailFragment fragment = new PlaylistDetailFragment();
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

    public PlaylistDetailFragment() {
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

        setHasOptionsMenu(true);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.new_playlist_details_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.inputs = PlaylistDetailsInputs.create();
        this.adapter = newPlaylistDetailsAdapterFactory.create(this, playlistDetailsHeaderRendererFactory.create(inputs));
        this.collectionRenderer = new CollectionRenderer<>(adapter, PlaylistDetailOtherPlaylistsItem::isTheSameItem, Object::equals, new DefaultEmptyStateProvider(), false, true);
        this.collectionRenderer.attach(view, false, new SmoothLinearLayoutManager(view.getContext()));
        this.itemTouchHelper = new ItemTouchHelper(touchCallbackFactory.create(this));

        final View detailView = view.findViewById(R.id.playlist_details);
        final boolean showInlineHeader = detailView == null;

        presenter.connect(inputs);
        subscription = new CompositeSubscription();
        subscription.addAll(

                presenter.viewModel()
                         .observeOn(AndroidSchedulers.mainThread())
                         .filter(ignored -> !skipModelUpdates)
                         .subscribe(data -> {
                             bindViews(detailView, data);
                             bindItems(showInlineHeader, data);
                         }),

                collectionRenderer.onRefresh()
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(aVoid -> inputs.refresh()),

                presenter.goToCreator()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(urn -> navigator.legacyOpenProfile(getActivity(), urn)),

                presenter.goToContentUpsell()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(ignored -> navigator.openUpgrade(getContext(), UpsellContext.PREMIUM_CONTENT)),

                presenter.goToOfflineUpsell()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(ignored -> navigator.openUpgrade(getContext(), UpsellContext.OFFLINE)),

                presenter.onRepostResult()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(new RepostResultSubscriber(view.getContext())),

                presenter.onRequestingPlaylistDeletion()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(urn -> DeletePlaylistDialogFragment.show(getFragmentManager(), urn)),

                presenter.onShowDisableOfflineCollectionConfirmation()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(data -> ConfirmRemoveOfflineDialogFragment.showForPlaylist(getFragmentManager(), data.first(), data.second().getPromotedSourceInfo())),

                presenter.onGoBack()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(ignored -> getActivity().onBackPressed()),

                presenter.onShare()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(shareOptions -> shareOperations.share(getContext(), shareOptions)),

                presenter.onShowOfflineStorageErrorDialog()
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(ignored -> OfflineStorageErrorDialog.show(getActivity().getSupportFragmentManager()))

        );

        if (showInlineHeader) {
            headerAnimator = headerAnimatorFactory.create(ButterKnife.findById(view, R.id.toolbar_id),
                                                          ButterKnife.findById(view, R.id.toolbar_shadow),
                                                          ButterKnife.findById(view, R.id.top_gradient),
                                                          ButterKnife.findById(view, R.id.system_bars_holder));
            headerAnimator.attach(recyclerView(), adapter);
        }
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
            inputs.onExitEditMode();
            return true;
        }
        return false;
    }

    private RecyclerView recyclerView() {
        return ButterKnife.findById(getActivity(), R.id.ak_recycler_view);
    }

    private void bindItems(boolean showInlineHeader, AsyncViewModel<PlaylistDetailsViewModel> data) {
        collectionRenderer.render(LegacyModelConverter.convert(showInlineHeader, data));
        bindItemsGestures(data);
    }

    private void bindItemsGestures(AsyncViewModel<PlaylistDetailsViewModel> syncModel) {
        final Optional<PlaylistDetailsViewModel> data = syncModel.data();
        if (data.isPresent() && data.get().metadata().isInEditMode()) {
            itemTouchHelper.attachToRecyclerView(recyclerView());
        } else {
            itemTouchHelper.attachToRecyclerView(null);
        }
    }

    private void bindViews(@Nullable View detailView, AsyncViewModel<PlaylistDetailsViewModel> asyncViewModel) {
        if (asyncViewModel.data().isPresent()) {
            PlaylistDetailsViewModel data = asyncViewModel.data().get();
            bindToolBar(data);
            bindHeader(data.metadata().isInEditMode());

            if (detailView != null) {
                // landscape tablet with side by side details
                playlistEngagementsRenderer.bind(detailView, inputs, data.metadata());
                playlistCoverRenderer.bind(detailView,
                                           data.metadata(),
                                           inputs::onHeaderPlayButtonClicked,
                                           inputs::onCreatorClicked);
            }

        }
    }

    private void bindHeader(boolean isInEditMode) {
        headerScrollHelper.setIsEditing(isInEditMode);

        if (headerAnimator != null) {
            headerAnimator.setIsInEditMode(isInEditMode, recyclerView());
        }
    }

    private void bindToolBar(PlaylistDetailsViewModel data) {
        toolbarView.setPlaylist(data.metadata());
    }

    @Override
    public void onDestroyView() {
        presenter.disconnect();
        subscription.unsubscribe();
        collectionRenderer.detach();

        if (headerAnimator != null) {
            headerAnimator.detatch(recyclerView(), adapter);
            headerAnimator = null;
        }
        itemTouchHelper = null;

        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }

    @Override
    public void onDestroy() {
        toolbarView = null;
        super.onDestroy();
    }

    @Override
    public void onItemClicked(PlaylistDetailTrackItem trackItem) {
        inputs.onItemTriggered(trackItem);
    }

    @Override
    public void onHandleTouched(RecyclerView.ViewHolder holder) {
        itemTouchHelper.startDrag(holder);
    }

    @Override
    public void onUpsellItemDismissed(PlaylistDetailUpsellItem item) {
        inputs.onItemDismissed(item);

        final int position = adapter.getItems().indexOf(item);
        adapter.removeItem(position);
        adapter.notifyItemRemoved(position);
    }

    @Override
    public void onUpsellItemClicked(PlaylistDetailUpsellItem item) {
        inputs.onItemTriggered(item);
    }

    @Override
    public void onUpsellItemPresented() {
        presenter.firePlaylistTracksUpsellImpression();
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

    /***
     * This is logic that could easily be moved into the Presenter to get rid of the legacy model, and properly tested. That is the next step
     */
    static class LegacyModelConverter {

        static CollectionRendererState<PlaylistDetailItem> convert(boolean useInlineHeader, AsyncViewModel<PlaylistDetailsViewModel> asyncViewModel) {
            final CollectionLoadingState loadingState = CollectionLoadingState
                    .builder()
                    .nextPageError(asyncViewModel.error())
                    .isRefreshing(asyncViewModel.isRefreshing())
                    .hasMorePages(false)
                    .build();
            final List<PlaylistDetailItem> items = toLegacyModelItems(asyncViewModel, useInlineHeader);
            return CollectionRendererState.create(loadingState, items);
        }

        private static List<PlaylistDetailItem> toLegacyModelItems(AsyncViewModel<PlaylistDetailsViewModel> asyncViewModel, boolean inlineHeader) {
            Optional<PlaylistDetailsViewModel> viewModelOpt = asyncViewModel.data();
            if (viewModelOpt.isPresent()) {
                PlaylistDetailsViewModel viewModel = viewModelOpt.get();
                EmptyStatus emptyStatus = EmptyStatus.fromErrorAndLoading(asyncViewModel.error(), asyncViewModel.isLoadingNextPage());
                PlaylistDetailEmptyItem emptyItem = new PlaylistDetailEmptyItem(emptyStatus, viewModel.metadata().isOwner());
                return viewModel.metadata().isInEditMode() || !inlineHeader ? viewModel.itemsWithoutHeader(emptyItem)
                                                                            : viewModel.itemsWithHeader(emptyItem);

            } else {
                EmptyStatus emptyStatus = EmptyStatus.fromErrorAndLoading(asyncViewModel.error(), true);
                PlaylistDetailEmptyItem emptyItem = new PlaylistDetailEmptyItem(emptyStatus, false);

                return inlineHeader ? Arrays.asList(new PlaylistDetailsHeaderItem(Optional.absent()), emptyItem)
                                    : Collections.singletonList(emptyItem);
            }
        }
    }
}
