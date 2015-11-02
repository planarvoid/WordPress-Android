package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import javax.inject.Inject;

class SearchPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {

    private static final int SUGGESTIONS_VIEW_INDEX = 0;
    private static final int RESULTS_VIEW_INDEX = 1;

    private EditText searchTextView;
    private ListView searchListView;
    private ViewFlipper searchViewFlipper;
    private FragmentManager fragmentManager;

    private final SuggestionsAdapter adapter;
    private final SuggestionsHelper suggestionsHelper;

    @Inject
    SearchPresenter(SuggestionsAdapter adapter, SuggestionsHelperFactory suggestionsHelperFactory) {
        this.adapter = adapter;
        this.suggestionsHelper = suggestionsHelperFactory.create(adapter);
    }

    @Override
    public void onAttach(Fragment fragment, Activity activity) {
        super.onAttach(fragment, activity);
        this.fragmentManager = fragment.getFragmentManager();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        this.fragmentManager = null;
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        setupViews(fragment, view);
    }

    private void setupViews(Fragment fragment, View fragmentView) {
        addSearchViewToToolbar(fragment);
        setupListView(fragmentView);
        setupViewFlipper(fragmentView);
        setSearchListeners();
    }

    private void setupListView(View fragmentView) {
        searchListView = (ListView) fragmentView.findViewById(android.R.id.list);
        searchListView.setAdapter(adapter);
    }

    private void setupViewFlipper(View fragmentView) {
        final Context context = fragmentView.getContext();
        searchViewFlipper = (ViewFlipper) fragmentView.findViewById(R.id.search_view_flipper);
        searchViewFlipper.setInAnimation(AnimationUtils.loadAnimation(context, R.anim.activity_open_enter));
        searchViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(context, R.anim.activity_open_exit));
    }

    private void addSearchViewToToolbar(Fragment fragment) {
        final Toolbar toolbar = (Toolbar) fragment.getActivity().findViewById(R.id.toolbar_id);
        searchTextView = (EditText) ((LayoutInflater) fragment.getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.search_text_view, toolbar, false);
        toolbar.addView(searchTextView);
    }

    private void setSearchListeners() {
        searchTextView.addTextChangedListener(new SearchWatcher());
        searchTextView.setOnClickListener(new SearchViewClickListener());
        searchTextView.setOnEditorActionListener(new SearchActionListener());
        searchListView.setOnItemClickListener(new SearchResultClickListener());
    }

    private void performSearch() {
        deactivateSearchView();
        showResultsFor(searchTextView.getText().toString());
    }

    private void deactivateSearchView() {
        searchTextView.setFocusable(false);
    }

    private void displaySearchView(int searchViewIndex) {
        if (searchViewFlipper.getDisplayedChild() != searchViewIndex) {
            searchViewFlipper.setDisplayedChild(searchViewIndex);
        }
    }

    private void showResultsFor(String query) {
        final TabbedSearchFragment searchResults = TabbedSearchFragment.newInstance(query);
        fragmentManager.beginTransaction().replace(R.id.search_results_container, searchResults).commit();
        displaySearchView(RESULTS_VIEW_INDEX);
    }

    private class SearchViewClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            displaySearchView(SUGGESTIONS_VIEW_INDEX);
            searchTextView.setFocusableInTouchMode(true);
            searchTextView.setFocusable(true);
            searchTextView.requestFocus();
        }
    }

    private class SearchWatcher implements TextWatcher {

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (Strings.isNotBlank(charSequence.toString().trim())) {
                adapter.showSuggestionsFor(charSequence);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (Strings.isBlank(editable.toString().trim())) {
                adapter.clearSuggestions();
            }
        }
    }

    private class SearchResultClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (adapter.isSearchItem(position)) {
                performSearch();
            } else {
                suggestionsHelper.launchSuggestion(view.getContext(), position);
            }
        }
    }

    private class SearchActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        }
    }
}
