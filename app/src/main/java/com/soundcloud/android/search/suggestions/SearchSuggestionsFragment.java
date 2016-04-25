package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SuggestionListener;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recyclerview_with_emptyview, container, false);
    }

    public void showSuggestionsFor(String query) {
        presenter.showSuggestionsFor(query);
    }

    @Override
    public void onDataChanged(boolean isEmpty) {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onDataChanged(isEmpty);
        }
    }

    @Override
    public void onScrollChanged() {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onScrollChanged();
        }
    }

    @Override
    public void onSearchClicked(String searchQuery) {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onSearchClicked(searchQuery);
        }
    }

    @Override
    public void onTrackClicked(Urn trackUrn) {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onTrackClicked(trackUrn);
        }
    }

    @Override
    public void onUserClicked(Urn userUrn) {
        if (getActivity() instanceof SuggestionListener) {
            ((SuggestionListener) getActivity()).onUserClicked(userUrn);
        }
    }
}
