package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
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
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.CollectionViewFragment;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.lightcycle.LightCycle;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class NewPlaylistDetailFragment extends CollectionViewFragment<PlaylistDetailTrackItem>
        implements TrackItemMenuPresenter.RemoveTrackListener, RefreshableScreen, PlaylistEngagementsRenderer.OnEngagementListener {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";
    public static final String EXTRA_AUTOPLAY = "autoplay";

    @Inject NewPlaylistDetailsPresenterFactory playlistPresenterFactory;
    @Inject NewPlaylistDetailsAdapterFactory adapterFactory;
    @Inject PlaylistEngagementsRenderer playlistEngagementsRenderer;
    @Inject PlaylistCoverRenderer playlistCoverRenderer;
    @Inject PlaylistTrackItemRendererFactory trackItemRendererFactory;
    @Inject PlaylistDetailTrackItemRendererFactory detailTrackItemRendererFactory;

    @Inject @LightCycle NewPlaylistDetailHeaderScrollHelper headerScrollHelper;
    @Inject PlaylistDetailToolbarViewFactory toolbarViewFactory;
    private PlaylistDetailToolbarView toolbarView;

    @Inject BaseLayoutHelper baseLayoutHelper;

    private NewPlaylistDetailsPresenter presenter;

    private View view;

    public NewPlaylistDetailFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = playlistPresenterFactory.create(getArguments().getParcelable(EXTRA_URN));
        presenter.connect();
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        baseLayoutHelper.setupActionBar(((AppCompatActivity) getActivity()));
        toolbarView = toolbarViewFactory.create(presenter);
        bind(toolbarView);
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
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return ButterKnife.findById(getView(), R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{ButterKnife.findById(getView(), R.id.ak_recycler_view)}; // revisit
    }

    @Override
    protected void onRefresh() {
        presenter.refresh();
    }

    @Override
    protected RecyclerItemAdapter<PlaylistDetailTrackItem, RecyclerView.ViewHolder> createAdapter() {
        final PlaylistTrackItemRenderer trackItemRenderer = trackItemRendererFactory.create(this);
        final PlaylistDetailTrackItemRenderer playlistTrackItemRenderer = detailTrackItemRendererFactory.create(trackItemRenderer);
        return adapterFactory.create(playlistTrackItemRenderer);
    }

    @Override
    protected Observable<Iterable<PlaylistDetailTrackItem>> items() {
        return modelUpdates()
                .doOnNext(playlistDetailsViewModelAsyncViewModel -> {
                    final PlaylistDetailsViewModel data = playlistDetailsViewModelAsyncViewModel.data();
                    playlistCoverRenderer.bind(view, data.metadata(), this::onHeaderPlay, this::onGotoCreator);
                    playlistEngagementsRenderer.bind(view, data.metadata(), this);
                    toolbarView.setPlaylist(data.metadata());
                })
                .map(viewModel -> viewModel.data().tracks());
    }

    private void onHeaderPlay() {

    }

    private void onGotoCreator() {

    }

    private Observable<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates() {
        return presenter
                .viewModel()
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    protected Observable<Boolean> isRefreshing() {
        return modelUpdates()
                .map(AsyncViewModel::isRefreshing);
    }

    @Override
    public void onDestroyView() {
        view = null;
        toolbarView = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        presenter.disconnect();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.new_playlist_details_fragment, container, false);
    }

    public static Fragment create(Urn playlistUrn, Screen screen, SearchQuerySourceInfo searchInfo,
                                  PromotedSourceInfo promotedInfo, boolean autoplay) {
        NewPlaylistDetailFragment fragment = new NewPlaylistDetailFragment();
        fragment.setArguments(createBundle(playlistUrn, screen, searchInfo, promotedInfo, autoplay));
        return fragment;
    }

    @Override
    public void onPlaylistTrackRemoved(Urn track) {

    }

    @VisibleForTesting
    static Bundle createBundle(Urn playlistUrn,
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

    /**
     * TODO :
     * - Screen Tracking
     */



    @Override
    public void onPlayNext(Urn playlistUrn) {

    }

    @Override
    public void onToggleLike(boolean isLiked) {

    }

    @Override
    public void onToggleRepost(boolean isReposted, boolean showResultToast) {

    }

    @Override
    public void onShare() {

    }

    @Override
    public void onMakeOfflineAvailable(boolean isMarkedForOffline) {

    }

    @Override
    public void onUpsell(Context context) {

    }

    @Override
    public void onOverflowUpsell(Context context) {

    }

    @Override
    public void onOverflowUpsellImpression() {

    }

    @Override
    public void onPlayShuffled() {

    }

    @Override
    public void onDeletePlaylist() {

    }

    @Override
    public void onEditPlaylist() {

    }

}
