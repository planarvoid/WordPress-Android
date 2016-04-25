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

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.*;

public class SearchSuggestionsFragment extends LightCycleSupportFragment<SearchSuggestionsFragment>
    implements SearchListener {

    public static final String TAG = "suggestions_search";

    @Inject @LightCycle SearchSuggestionsPresenter presenter;

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

    @Override
    public void onSearchClicked(String searchQuery) {
        if (getActivity() instanceof SearchListener) {
            ((SearchListener) getActivity()).onSearchClicked(searchQuery);
        }
    }
}
