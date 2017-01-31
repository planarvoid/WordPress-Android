package com.soundcloud.android.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.PlaylistTrackItemRendererFactory;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.CollectionViewFragment;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

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

public class NewPlaylistDetailFragment extends CollectionViewFragment<PlaylistDetailsViewModel, PlaylistDetailTrackItem>
        implements TrackItemMenuPresenter.RemoveTrackListener {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";
    public static final String EXTRA_AUTOPLAY = "autoplay";

    @Inject @LightCycle NewPlaylistDetailHeaderScrollHelper headerScrollHelper;
    @Inject NewPlaylistDetailsPresenterFactory playlistPresenterFactory;
    @Inject NewPlaylistDetailsAdapterFactory adapterFactory;
    @Inject PlaylistEngagementsRenderer playlistEngagementsRenderer;
    @Inject PlaylistCoverRenderer playlistCoverRenderer;
    @Inject PlaylistTrackItemRendererFactory trackItemRendererFactory;
    @Inject PlaylistDetailTrackItemRendererFactory detailTrackItemRendererFactory;
    @Inject Navigator navigator;
    @Inject PlaylistDetailToolbarViewFactory toolbarViewFactory;
    @Inject BaseLayoutHelper baseLayoutHelper;

    private PlaylistDetailToolbarView toolbarView;
    private NewPlaylistDetailsPresenter presenter;
    private Subscription subscription;

    public NewPlaylistDetailFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        baseLayoutHelper.setupActionBar(((AppCompatActivity) getActivity()));
        toolbarView = toolbarViewFactory.create(presenter);
        bind(toolbarView);

        subscription = presenter.viewModel().subscribe(asyncViewModel -> {
            Optional<PlaylistDetailsViewModel> dataOpt = asyncViewModel.data();
            if (dataOpt.isPresent()) {
                bindMetadata(view, dataOpt.get());
            }
        });
    }

    private void bindMetadata(View view, PlaylistDetailsViewModel data) {
        playlistCoverRenderer.bind(view, data.metadata(), presenter::onHeaderPlayButtonClicked, presenter::onCreatorClicked);
        playlistEngagementsRenderer.bind(view, data.metadata(), presenter);
        toolbarView.setPlaylist(data.metadata());
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
    protected Observable<AsyncViewModel<PlaylistDetailsViewModel>> modelUpdates() {
        return presenter
                .viewModel()
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    protected Func1<PlaylistDetailsViewModel, Iterable<PlaylistDetailTrackItem>> viewModelToItems() {
        return PlaylistDetailsViewModel::tracks;
    }

    @Override
    public void onDestroyView() {
        toolbarView = null;
        subscription.unsubscribe();
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
}
