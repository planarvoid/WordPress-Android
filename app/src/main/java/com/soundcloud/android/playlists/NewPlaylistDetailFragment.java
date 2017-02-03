package com.soundcloud.android.playlists;

import static android.support.v4.view.MotionEventCompat.getActionMasked;
import static com.soundcloud.java.collections.Lists.transform;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.tracks.TrackItemView;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.CollectionViewFragment;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class NewPlaylistDetailFragment extends CollectionViewFragment<PlaylistDetailsViewModel, PlaylistDetailTrackItem, NewPlaylistDetailFragment.MyViewHolder>
        implements TrackItemMenuPresenter.RemoveTrackListener, RefreshableScreen, AppBarLayout.OnOffsetChangedListener {

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
    @Inject Navigator navigator;

    private final ItemTouchHelper itemTouchHelper;
    private PlaylistDetailToolbarView toolbarView;
    private NewPlaylistDetailsPresenter presenter;

    private boolean skipModelUpdates;
    private View view;
    private CompositeSubscription subscription;


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
        this.itemTouchHelper = new ItemTouchHelper(touchCallbackFactory.create(this));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = playlistPresenterFactory.create(getArguments().getParcelable(EXTRA_URN),
                                                    Screen.fromBundle(getArguments()).get(),
                                                    getArguments().getParcelable(EXTRA_QUERY_SOURCE_INFO),
                                                    getArguments().getParcelable(EXTRA_PROMOTED_SOURCE_INFO));
        presenter.connect();

        presenter.goToCreator().subscribe(urn -> {
            navigator.legacyOpenProfile(getActivity(), urn);
        });

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.new_playlist_details_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        toolbarView = toolbarViewFactory.create(presenter, actionBar());
        bind(toolbarView);

        animateLayoutChangesInAdapterCells();
        doNotPropagateAnimationsToTheAppBar();

        subscription = new CompositeSubscription();
        subscription.addAll(

                modelUpdates().subscribe(asyncViewModel -> {
                    Optional<PlaylistDetailsViewModel> dataOpt = asyncViewModel.data();
                    if (dataOpt.isPresent()) {
                        bindViews(dataOpt.get());
                    }
                }),

                onRefresh.subscribe(aVoid -> {
                    presenter.refresh();
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

    // Let the layout for tracks animates itself
    private void animateLayoutChangesInAdapterCells() {
        ((SimpleItemAnimator) recyclerView().getItemAnimator()).setSupportsChangeAnimations(false);
    }

    private ActionBar actionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    // Hack : this prevents the appbar from scrolling strangely when
    // the view disappearance animation run.
    // http://stackoverflow.com/questions/36064424/animate-layout-changes-broken-in-nested-layout-with-collapsingtoolbarlayout
    private void doNotPropagateAnimationsToTheAppBar() {
        final LayoutTransition layoutTransition = playlistDetailsView().getLayoutTransition();
        layoutTransition.setAnimateParentHierarchy(false);
    }

    private ViewGroup playlistDetailsView() {
        return ButterKnife.findById(getActivity(), R.id.playlist_details);
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
    public void onResume() {
        super.onResume();
        appBarLayout().addOnOffsetChangedListener(this);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return ButterKnife.findById(getActivity(), R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{recyclerView()}; // revisit
    }

    private RecyclerView recyclerView() {
        return ButterKnife.findById(getActivity(), R.id.ak_recycler_view);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        enableSwipeToRefresh(verticalOffset == 0);
    }

    private void enableSwipeToRefresh(boolean isEnabled) {
        swipeRefreshLayout.setEnabled(isEnabled);
    }

    @Override
    public void onPause() {
        super.onPause();
        appBarLayout().removeOnOffsetChangedListener(this);
    }

    private AppBarLayout appBarLayout() {
        return ButterKnife.findById(getActivity(), R.id.appbar);
    }

    @Override
    protected RecyclerItemAdapter<PlaylistDetailTrackItem, MyViewHolder> createAdapter() {
        final PlaylistTrackItemRenderer trackItemRenderer = trackItemRendererFactory.create(this);
        return new NewPlaylistDetailsAdapter(trackItemRenderer);
    }

    @Override
    protected void onNewItems(List<PlaylistDetailTrackItem> newItems) {
        final List<PlaylistDetailTrackItem> oldItems = adapter().getItems();
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AdapterDiffCallback(oldItems, newItems), true);

        populateAdapter(newItems);
        diffResult.dispatchUpdatesTo(adapter());
    }

    @Override
    protected Observable<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates() {
        return presenter
                .viewModel()
                .filter(ignored -> !skipModelUpdates)
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    protected Func1<PlaylistDetailsViewModel, List<PlaylistDetailTrackItem>> viewModelToItems() {
        return PlaylistDetailsViewModel::tracks;
    }

    private void bindViews(PlaylistDetailsViewModel data) {
        bindCover(data);
        bindEngagementBar(data);
        bindToolBar(data);
        bindEditMode(data);
    }

    private void bindEditMode(PlaylistDetailsViewModel data) {
        if (data.metadata().isInEditMode()) {
            itemTouchHelper.attachToRecyclerView(recyclerView());
        } else {
            itemTouchHelper.attachToRecyclerView(null);
        }
    }

    private void bindToolBar(PlaylistDetailsViewModel data) {
        toolbarView.setPlaylist(data.metadata());
    }

    private void bindEngagementBar(PlaylistDetailsViewModel data) {
        playlistEngagementsRenderer.bind(view, data.metadata(), presenter);
    }

    private void bindCover(PlaylistDetailsViewModel data) {
        playlistCoverRenderer.bind(view, data.metadata(), presenter::onHeaderPlayButtonClicked, presenter::onCreatorClicked);
    }

    @Override
    public void onDestroyView() {
        view = null;
        toolbarView = null;
        subscription.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        presenter.disconnect();
        super.onDestroy();
    }

    private void startDrag(MyViewHolder holder) {
        itemTouchHelper.startDrag(holder);
    }

    public void onDragStarted() {
        skipModelUpdates = true;
    }

    public void dragItem(int fromPosition, int toPosition) {
        Collections.swap(adapter().getItems(), fromPosition, toPosition);
        adapter().notifyItemMoved(fromPosition, toPosition);
    }

    public void onDragStopped() {
        skipModelUpdates = false;
        saveUpdates();
    }

    @Override
    public void onPlaylistTrackRemoved(Urn trackUrn) {
        final List<Urn> urns = transform(adapter().getItems(), PlaylistDetailTrackItem::getUrn);
        removeTrackAtPosition(urns.indexOf(trackUrn));
    }

    public void removeItem(int position) {
        removeTrackAtPosition(position);
    }

    private void removeTrackAtPosition(int trackPosition) {
        adapter().getItems().remove(trackPosition);
        adapter().notifyItemRemoved(trackPosition);
        saveUpdates();
    }

    private void saveUpdates() {
        presenter.actionUpdateTrackList(adapter().getItems());
    }

    static class AdapterDiffCallback extends DiffUtil.Callback {
        private final List<PlaylistDetailTrackItem> oldItems;
        private final List<PlaylistDetailTrackItem> newItems;

        public AdapterDiffCallback(List<PlaylistDetailTrackItem> oldItems, List<PlaylistDetailTrackItem> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            final PlaylistDetailTrackItem oldItem = oldItems.get(oldItemPosition);
            final PlaylistDetailTrackItem newItem = newItems.get(newItemPosition);
            return newItem.getUrn().equals(oldItem.getUrn());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final PlaylistDetailTrackItem oldItem = oldItems.get(oldItemPosition);
            final PlaylistDetailTrackItem newItem = newItems.get(newItemPosition);
            return newItem.equals(oldItem);
        }
    }

    class NewPlaylistDetailsAdapter extends RecyclerItemAdapter<PlaylistDetailTrackItem, MyViewHolder> {
        private final PlaylistTrackItemRenderer playlistTrackItemRenderer;

        NewPlaylistDetailsAdapter(PlaylistTrackItemRenderer playlistTrackItemRenderer) {
            this.playlistTrackItemRenderer = playlistTrackItemRenderer;
            this.playlistTrackItemRenderer.trackItemViewFactory().setLayoutId(R.layout.edit_playlist_track_item);
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return createViewHolder(playlistTrackItemRenderer.createItemView(parent));
        }

        @Override
        protected MyViewHolder createViewHolder(View itemView) {
            return new MyViewHolder(itemView);
        }

        private View.OnTouchListener createDragListener(MyViewHolder holder) {
            return (view, motionEvent) -> {
                if (getActionMasked(motionEvent) == MotionEvent.ACTION_DOWN) {
                    startDrag(holder);
                }
                return false;
            };
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            final PlaylistDetailTrackItem detailTrackItem = items.get(position);
            final View itemView = holder.itemView;
            playlistTrackItemRenderer.bindTrackView(position, itemView, detailTrackItem.trackItem());
            itemView.setOnClickListener(view -> {
                if (!detailTrackItem.inEditMode()) {
                    presenter.onPlayAtPosition(position);
                }
            });
            bindEditMode(holder, detailTrackItem, position);
        }

        // TODO eventually move to a cell renderer
        private void bindEditMode(MyViewHolder holder, PlaylistDetailTrackItem detailTrackItem, int position) {
            if (detailTrackItem.inEditMode()) {
                bindHandle(holder);
            } else {
                holder.handle().setVisibility(View.GONE);
            }
        }

        private void bindHandle(MyViewHolder holder) {
            holder.handle().setOnTouchListener(createDragListener(holder));
            holder.handle().setVisibility(View.VISIBLE);
            holder.overflow().setVisibility(View.GONE);
            holder.preview().setVisibility(View.GONE);
            holder.hideDuration();
        }

        @Override
        public int getBasicItemViewType(int position) {
            return 0;
        }

    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        MyViewHolder(View itemView) {
            super(itemView);
        }

        public View preview() {
            return ButterKnife.findById(itemView, R.id.preview_indicator);
        }


        ImageView overflow() {
            return ButterKnife.findById(itemView, R.id.overflow_button);
        }

        ImageView handle() {
            return ButterKnife.findById(itemView, R.id.drag_handle);
        }

        void hideDuration() {
            trackItemView().hideDuration();
        }

        private TrackItemView trackItemView() {
            return (TrackItemView) itemView.getTag();
        }

    }
}
