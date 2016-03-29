package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchSuggestionsFragment extends LightCycleSupportFragment<SearchSuggestionsFragment> {

    public static final String TAG = "suggestions_search";

    static final String EXTRA_QUERY = "query";

    @Inject @LightCycle SearchSuggestionsPresenter presenter;

    public static SearchSuggestionsFragment create(String query) {
        final Bundle bundle = new Bundle();
        bundle.putString(EXTRA_QUERY, query);
        final SearchSuggestionsFragment fragment = new SearchSuggestionsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public SearchSuggestionsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recyclerview_with_emptyview, container, false);
    }

    public void showSuggestionsFor(String query) {
        presenter.showSuggestionsFor(query);
    }

    public void clearSuggestions() {
        presenter.clearSuggestions();
    }
}
