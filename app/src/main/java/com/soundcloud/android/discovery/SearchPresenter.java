package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.utils.KeyboardHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import javax.inject.Inject;

class SearchPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements SearchIntentResolver.DeepLinkListener {

    private static final int SUGGESTIONS_VIEW_INDEX = 0;
    private static final int RESULTS_VIEW_INDEX = 1;

    private static final String CURRENT_DISPLAYING_VIEW_KEY = "currentDisplayingView";

    private EditText searchTextView;
    private ImageView searchCloseView;
    private ListView searchListView;
    private ViewFlipper searchViewFlipper;
    private View toolbarElevation;
    private Window window;
    private FragmentManager fragmentManager;

    private final SearchIntentResolver intentResolver;
    private final SearchTracker tracker;
    private final Resources resources;
    private final SuggestionsAdapter adapter;
    private final SuggestionsHelper suggestionsHelper;
    private final EventBus eventBus;
    private final KeyboardHelper keyboardHelper;

    @Inject
    SearchPresenter(SearchIntentResolverFactory intentResolverFactory,
                    SearchTracker tracker,
                    Resources resources,
                    SuggestionsAdapter adapter,
                    SuggestionsHelperFactory suggestionsHelperFactory,
                    EventBus eventBus,
                    KeyboardHelper keyboardHelper) {
        this.intentResolver = intentResolverFactory.create(this);
        this.tracker = tracker;
        this.resources = resources;
        this.adapter = adapter;
        this.suggestionsHelper = suggestionsHelperFactory.create(adapter);
        this.adapter.registerDataSetObserver(new SuggestionsVisibilityController());
        this.eventBus = eventBus;
        this.keyboardHelper = keyboardHelper;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.window = activity.getWindow();
        this.fragmentManager = activity.getSupportFragmentManager();
        setupViews(activity);
        if (bundle == null) {
            intentResolver.handle(activity, activity.getIntent());
        }
    }

    @Override
    public void onDeepLinkExecuted(String searchQuery) {
        searchTextView.setText(searchQuery);
        deactivateSearchView();
        showResultsFor(searchQuery);
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        bundle.putInt(CURRENT_DISPLAYING_VIEW_KEY, searchViewFlipper.getDisplayedChild());
        super.onSaveInstanceState(activity, bundle);
    }

    @Override
    public void onRestoreInstanceState(AppCompatActivity activity, Bundle bundle) {
        super.onRestoreInstanceState(activity, bundle);
        displaySearchView(bundle.getInt(CURRENT_DISPLAYING_VIEW_KEY));
    }

    private void setupViews(AppCompatActivity activity) {
        setupToolbar(activity);
        setupListView(activity);
        setupViewFlipper(activity);
        setSearchListeners();
        requestSearchFocus();
    }

    private void setupListView(Activity activity) {
        searchListView = (ListView) activity.findViewById(android.R.id.list);
        searchListView.setAdapter(adapter);
    }

    private void setupViewFlipper(Activity activity) {
        searchViewFlipper = (ViewFlipper) activity.findViewById(R.id.search_view_flipper);
        searchViewFlipper.setInAnimation(AnimationUtils.loadAnimation(activity, R.anim.activity_open_enter));
        searchViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(activity, R.anim.activity_open_exit));
    }

    private void setupToolbar(AppCompatActivity activity) {
        final Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_id);
        final ViewGroup searchView = (ViewGroup) ((LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.search_text_view, toolbar, false);
        final ActionBar actionBar = activity.getSupportActionBar();
        toolbarElevation = activity.findViewById(R.id.legacy_elevation);
        searchTextView = (EditText) searchView.findViewById(R.id.search_text);
        searchCloseView = (ImageView) searchView.findViewById(R.id.search_close);
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.addView(searchView);
    }

    private void requestSearchFocus() {
        final PlayerUIEvent currentPlayerState = eventBus.queue(EventQueue.PLAYER_UI).toBlocking().first();
        if (currentPlayerState.getKind() == PlayerUIEvent.PLAYER_COLLAPSED) {
            activateSearchView();
        }
    }

    private void setSearchListeners() {
        searchTextView.addTextChangedListener(new SearchWatcher());
        searchTextView.setOnClickListener(new SearchViewClickListener());
        searchTextView.setOnEditorActionListener(new SearchActionListener());
        searchListView.setOnItemClickListener(new SearchResultClickListener());
        searchListView.setOnScrollListener(new SuggestionsScrollListener());
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

    private void displaySearchView(int searchViewIndex) {
        setElevation(searchViewIndex);
        if (searchViewFlipper.getDisplayedChild() != searchViewIndex) {
            searchViewFlipper.setDisplayedChild(searchViewIndex);
        }
        if (searchViewIndex == RESULTS_VIEW_INDEX) {
            hideKeyboard();
        }
    }

    private void setElevation(int searchViewIndex) {
        if (searchViewIndex == RESULTS_VIEW_INDEX) {
            ViewCompat.setElevation(searchViewFlipper, (int) resources.getDimension(R.dimen.toolbar_elevation));
            toolbarElevation.setVisibility(View.INVISIBLE);
        } else {
            ViewCompat.setElevation(searchViewFlipper, 0);
            toolbarElevation.setVisibility(View.VISIBLE);
        }
    }

    private void showResultsFor(String query) {
        final TabbedSearchFragment searchResults = TabbedSearchFragment.newInstance(query);
        fragmentManager
                .beginTransaction()
                .replace(R.id.search_results_container, searchResults, TabbedSearchFragment.TAG)
                .commit();
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

    private void showKeyboard() {
        keyboardHelper.show(window, searchTextView);
    }

    private void hideKeyboard() {
        keyboardHelper.hide(window, searchTextView);
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
            activateSearchView();
            displaySearchView(SUGGESTIONS_VIEW_INDEX);
            tracker.trackScreenEvent();
        }
    }

    private class SuggestionsScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                hideKeyboard();
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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
