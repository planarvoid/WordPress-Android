package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_NEXT_HREF;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY;
import static com.soundcloud.android.search.SearchPremiumResultsActivity.EXTRA_SEARCH_TYPE;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.ArrayList;

public class SearchPremiumResultsFragment extends LightCycleSupportFragment implements RefreshableScreen {

    @Inject @LightCycle SearchPremiumResultsPresenter presenter;

    public SearchPremiumResultsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    static SearchPremiumResultsFragment create(String searchQuery,
                                               int searchType,
                                               ArrayList<PropertySet> premiumContentSourceSet,
                                               Link nextHref) {
        final Bundle bundle = new Bundle();
        bundle.putString(EXTRA_SEARCH_QUERY, searchQuery);
        bundle.putInt(EXTRA_SEARCH_TYPE, searchType);
        bundle.putParcelableArrayList(EXTRA_PREMIUM_CONTENT_RESULTS, premiumContentSourceSet);
        bundle.putParcelable(EXTRA_PREMIUM_CONTENT_NEXT_HREF, nextHref);
        final SearchPremiumResultsFragment fragment = new SearchPremiumResultsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
