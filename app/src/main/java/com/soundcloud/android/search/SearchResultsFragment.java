package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchResultsFragment extends LightCycleSupportFragment<SearchResultsFragment>
        implements RefreshableScreen {

    static final String EXTRA_ARGS = "args";

    @Inject @LightCycle SearchResultsPresenter presenter;


    public static SearchResultsFragment createInTab(SearchType type,
                                                    String apiQuery,
                                                    String userQuery,
                                                    Optional<Urn> queryUrn,
                                                    Optional<Integer> queryPosition,
                                                    boolean publishSearchSubmissionEvent) {
        return create(type, apiQuery, userQuery, queryUrn, queryPosition, publishSearchSubmissionEvent, false);
    }

    public static SearchResultsFragment createForViewAll(SearchType type,
                                                         String apiQuery,
                                                         String userQuery,
                                                         Optional<Urn> queryUrn,
                                                         Optional<Integer> queryPosition,
                                                         boolean isPremium) {
        return create(type, apiQuery, userQuery, queryUrn, queryPosition, false, isPremium);
    }

    private static SearchResultsFragment create(SearchType type,
                                                String apiQuery,
                                                String userQuery,
                                                Optional<Urn> queryUrn,
                                                Optional<Integer> queryPosition,
                                                boolean publishSearchSubmissionEvent,
                                                boolean isPremium) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_ARGS, SearchFragmentArgs.create(type, apiQuery, userQuery, queryUrn, queryPosition, publishSearchSubmissionEvent, isPremium));
        final SearchResultsFragment fragment = new SearchResultsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public SearchResultsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView(), presenter.getEmptyView()};
    }
}
