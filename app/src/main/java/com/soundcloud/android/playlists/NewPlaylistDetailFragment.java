package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.CollectionViewFragment;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.lightcycle.LightCycle;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class NewPlaylistDetailFragment extends CollectionViewFragment<PlaylistDetailItem>
        implements TrackItemMenuPresenter.RemoveTrackListener, RefreshableScreen {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";
    public static final String EXTRA_AUTOPLAY = "autoplay";

    @Inject NewPlaylistDetailsPresenterFactory playlistPresenterFactory;
    @Inject NewPlaylistDetailsAdapterFactory adapterFactory;

    @Inject PlaylistTrackItemRendererFactory trackItemRendererFactory;
    @Inject PlaylistDetailTrackItemRendererFactory detailTrackItemRendererFactory;

    @Inject @LightCycle PlaylistHeaderPresenter playlistHeaderPresenter;

    private NewPlaylistDetailsPresenter presenter;

    public NewPlaylistDetailFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = playlistPresenterFactory.create(getArguments().getParcelable(EXTRA_URN));
        presenter.connect();
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
    protected RecyclerItemAdapter<PlaylistDetailItem, RecyclerView.ViewHolder> createAdapter() {
        final PlaylistTrackItemRenderer trackItemRenderer = trackItemRendererFactory.create(this);
        final PlaylistDetailTrackItemRenderer playlistTrackItemRenderer = detailTrackItemRendererFactory.create(trackItemRenderer);
        return adapterFactory.create(playlistTrackItemRenderer);
    }

    @Override
    protected Observable<Iterable<PlaylistDetailItem>> items() {
        return modelUpdates()
                .doOnNext(playlistDetailsViewModelAsyncViewModel -> playlistHeaderPresenter.setPlaylist(playlistDetailsViewModelAsyncViewModel.data().playlistWithTracks(), PlaySessionSource.EMPTY))
                .map(viewModel -> viewModel.data().playlistDetailItems());
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

}
