package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlaylistDetailFragment extends LightCycleSupportFragment<PlaylistDetailFragment> implements RefreshableScreen {

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";
    public static final String EXTRA_AUTOPLAY = "autoplay";

    @Inject @LightCycle PlaylistPresenter playlistPresenter;

    public PlaylistDetailFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlist_details_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        playlistPresenter.setScreen(Screen.fromBundle(getArguments()).get());
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{playlistPresenter.getRecyclerView(), playlistPresenter.getEmptyView()};
    }

    public static Fragment create(Urn playlistUrn, Screen screen, SearchQuerySourceInfo searchInfo,
                                  PromotedSourceInfo promotedInfo, boolean autoplay) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        fragment.setArguments(createBundle(playlistUrn, screen, searchInfo, promotedInfo, autoplay));
        return fragment;
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
