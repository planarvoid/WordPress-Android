package com.soundcloud.android.discovery;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;

import com.soundcloud.android.R;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import javax.inject.Inject;

class SearchPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {

    private static final int SUGGESTIONS_VIEW_INDEX = 0;
    private static final int RESULTS_VIEW_INDEX = 1;

    private EditText searchTextView;
    private ImageView searchCloseView;
    private ListView searchListView;
    private ViewFlipper searchViewFlipper;
    private Window window;
    private FragmentManager fragmentManager;
    private InputMethodManager inputMethodManager;

    private final SuggestionsAdapter adapter;
    private final SuggestionsHelper suggestionsHelper;

    @Inject
    SearchPresenter(final SuggestionsAdapter adapter, SuggestionsHelperFactory suggestionsHelperFactory) {
        this.adapter = adapter;
        this.suggestionsHelper = suggestionsHelperFactory.create(adapter);
        this.adapter.registerDataSetObserver(new SuggestionsVisibilityController());
    }

    @Override
    public void onAttach(Fragment fragment, Activity activity) {
        super.onAttach(fragment, activity);
        this.window = activity.getWindow();
        this.fragmentManager = fragment.getFragmentManager();
        this.inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onDestroy(Fragment fragment) {
        this.window = null;
        this.fragmentManager = null;
        this.inputMethodManager = null;
        super.onDestroy(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        setupViews(fragment, view);
    }

    private void setupViews(Fragment fragment, View fragmentView) {
        setupToolbar(fragment);
        setupListView(fragmentView);
        setupViewFlipper(fragmentView);
        setSearchListeners();
        activateSearchView();
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

    private void setupToolbar(Fragment fragment) {
        final Toolbar toolbar = (Toolbar) fragment.getActivity().findViewById(R.id.toolbar_id);
        final ViewGroup searchView = (ViewGroup) ((LayoutInflater) fragment.getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.search_text_view, toolbar, false);
        searchTextView = (EditText) searchView.findViewById(R.id.search);
        searchCloseView = (ImageView) searchView.findViewById(R.id.search_close);
        toolbar.addView(searchView);
    }

    private void setSearchListeners() {
        searchTextView.addTextChangedListener(new SearchWatcher());
        searchTextView.setOnClickListener(new SearchViewClickListener());
        searchTextView.setOnEditorActionListener(new SearchActionListener());
        searchListView.setOnItemClickListener(new SearchResultClickListener());
        searchCloseView.setOnClickListener(new SearchCloseClickListener());
    }

    private void performSearch() {
        deactivateSearchView();
        showResultsFor(searchTextView.getText().toString());
    }

    private void activateSearchView() {
        searchTextView.setFocusable(true);
        searchTextView.setFocusableInTouchMode(true);
        searchTextView.requestFocus();
        showKeyboard();
    }

    private void deactivateSearchView() {
        searchTextView.setFocusable(false);
        searchTextView.setFocusableInTouchMode(false);
        searchTextView.clearFocus();
        hideKeyboard();
    }

    private void showKeyboard() {
        window.setSoftInputMode(SOFT_INPUT_ADJUST_NOTHING | SOFT_INPUT_STATE_VISIBLE);
        inputMethodManager.showSoftInput(searchTextView, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        window.setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        inputMethodManager.hideSoftInputFromWindow(searchTextView.getWindowToken(), 0);
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

    private void showSuggestionsFor(String query) {
        adapter.showSuggestionsFor(query);
    }

    private void clearSuggestions() {
        adapter.clearSuggestions();
    }

    private void showCloseButton() {
        searchCloseView.setVisibility(View.VISIBLE);
    }

    private void hideCloseButton() {
        searchCloseView.setVisibility(View.INVISIBLE);
    }

    private void showSearchListView() {
        searchListView.animate().alpha(1).start();
        searchListView.setVisibility(View.VISIBLE);
    }

    private void hideSearchListView() {
        searchListView.animate().alpha(0).start();
        searchListView.setVisibility(View.INVISIBLE);
    }

    private void showSearchViewResults() {
        searchViewFlipper.animate().alpha(1).start();
        searchViewFlipper.setVisibility(View.VISIBLE);
    }

    private void hideSearchViewResults() {
        searchViewFlipper.animate().alpha(0).start();
        searchViewFlipper.setVisibility(View.INVISIBLE);
    }

    private class SearchViewClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            displaySearchView(SUGGESTIONS_VIEW_INDEX);
            activateSearchView();
        }
    }

    private class SearchWatcher implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            final String searchQuery = charSequence.toString().trim();
            if (Strings.isNotBlank(searchQuery)) {
                showSuggestionsFor(searchQuery);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (Strings.isBlank(searchTextView.getText().toString().trim())) {
                clearSuggestions();
                hideCloseButton();
                hideSearchViewResults();
            } else {
                showCloseButton();
                showSearchViewResults();
            }
        }
    }

    private class SearchResultClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (adapter.isSearchItem(position)) {
                performSearch();
            } else {
                deactivateSearchView();
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

    private class SearchCloseClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            searchTextView.setText(Strings.EMPTY);
            clearSuggestions();
            hideCloseButton();
            showKeyboard();
            displaySearchView(SUGGESTIONS_VIEW_INDEX);
        }
    }

    private class SuggestionsVisibilityController extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            if (!adapter.isEmpty()) {
                showSearchListView();
            } else {
                hideSearchListView();
            }
        }
    }
}
