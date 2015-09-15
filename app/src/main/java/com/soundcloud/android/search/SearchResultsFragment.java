package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchResultsFragment extends LightCycleSupportFragment {

    static final String EXTRA_QUERY = "query";
    static final String EXTRA_TYPE = "type";
    static final String EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT = "publishSearchSubmissionEvent";

    @Inject @LightCycle SearchResultsPresenter searchPresenter;

    public static SearchResultsFragment create(int type, String query, boolean publishSearchSubmissionEvent) {
        final SearchResultsFragment fragment = new SearchResultsFragment();
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_TYPE, type);
        bundle.putString(EXTRA_QUERY, query);
        bundle.putBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, publishSearchSubmissionEvent);
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
}
