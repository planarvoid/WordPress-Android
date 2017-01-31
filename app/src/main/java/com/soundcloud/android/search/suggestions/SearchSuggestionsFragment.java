package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SuggestionListener;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchSuggestionsFragment extends LightCycleSupportFragment<SearchSuggestionsFragment>
        implements SuggestionListener {

    public static final String TAG = "suggestions_search";

    @Inject @LightCycle SearchSuggestionsPresenter presenter;

    public SearchSuggestionsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter.setSuggestionListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recyclerview_with_emptyview, container, false);
    }

    public void showSuggestionsFor(String query) {
        presenter.showSuggestionsFor(query);
    }

    @Override
    public void onScrollChanged() {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onScrollChanged();
        }
    }

    @Override
    public void onSearchClicked(String apiQuery, String userQuery) {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onSearchClicked(apiQuery, userQuery);
        }
    }

    @Override
    public void onSuggestionClicked() {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onSuggestionClicked();
        }
    }

    @Override
    public void onAutocompleteClicked(String query, String userQuery, String output, Optional<Urn> queryUrn, int position) {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onAutocompleteClicked(query, userQuery, output, queryUrn, position);
        }
    }
}
