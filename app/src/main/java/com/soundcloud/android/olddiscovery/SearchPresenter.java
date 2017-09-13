package com.soundcloud.android.olddiscovery;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.TopResultsConfig;
import com.soundcloud.android.deeplinks.UriResolveException;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.search.suggestions.SearchSuggestionsFragment;
import com.soundcloud.android.search.topresults.TopResultsFragment;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.KeyboardHelper;
import com.soundcloud.android.utils.TransitionUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import javax.inject.Inject;

public class SearchPresenter extends DefaultActivityLightCycle<AppCompatActivity>
        implements SearchIntentResolver.DeepLinkListener {

    public static final String SEARCH_RESULTS_TAG = "search_results";

    private static final int SUGGESTIONS_VIEW_INDEX = 0;
    private static final int RESULTS_VIEW_INDEX = 1;

    private static final String CURRENT_DISPLAYING_VIEW_KEY = "currentDisplayingView";

    private EditText searchTextView;
    private ImageView searchCloseView;
    private ViewFlipper searchViewFlipper;
    private View toolbarElevation;
    private Window window;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;

    private final SearchIntentResolver intentResolver;
    private final SearchTracker searchTracker;
    private final EventTracker eventTracker;
    private final ScreenProvider screenProvider;
    private final TopResultsConfig topResultsConfig;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final Resources resources;
    private final EventBus eventBus;
    private final KeyboardHelper keyboardHelper;

    @Inject
    SearchPresenter(SearchIntentResolverFactory intentResolverFactory,
                    SearchTracker searchTracker,
                    Resources resources,
                    EventBus eventBus,
                    KeyboardHelper keyboardHelper,
                    EventTracker eventTracker,
                    ScreenProvider screenProvider,
                    TopResultsConfig topResultsConfig,
                    PerformanceMetricsEngine performanceMetricsEngine) {
        this.intentResolver = intentResolverFactory.create(this);
        this.searchTracker = searchTracker;
        this.resources = resources;
        this.eventBus = eventBus;
        this.keyboardHelper = keyboardHelper;
        this.eventTracker = eventTracker;
        this.screenProvider = screenProvider;
        this.topResultsConfig = topResultsConfig;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.window = activity.getWindow();
        this.fragmentManager = activity.getSupportFragmentManager();
        setupBackground(activity);
        setupTransitionAnimation(window);
        setupViews(activity);
        if (bundle == null) {
            try {
                intentResolver.handle(activity, activity.getIntent());
            } catch (UriResolveException e) {
                AndroidUtils.showToast(activity, R.string.error_unknown_navigation);
                ErrorUtils.handleSilentException(e);
            }
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.window = null;
        this.fragmentManager = null;
        this.fragmentTransaction = null;
        super.onDestroy(activity);
    }

    @Override
    public void onDeepLinkExecuted(String searchQuery) {
        searchTextView.setText(searchQuery);
        performSearch(searchQuery, searchQuery);
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

    void onAutocompleteArrowClicked(String userQuery, String selectedSearchTerm, Optional<Urn> queryUrn, Optional<Integer> queryPosition) {
        showOutputText(Optional.of(selectedSearchTerm));
        eventTracker.trackSearch(SearchEvent.searchFormulationUpdate(screenProvider.getLastScreen(), userQuery, selectedSearchTerm, queryUrn, queryPosition));
    }

    void onScrollChanged() {
        hideKeyboard();
    }

    void performSearch(String apiQuery, String userQuery) {
        performSearch(apiQuery, userQuery, Optional.absent(), Optional.absent(), Optional.absent());
    }

    void performSearch(String apiQuery,
                       String userQuery,
                       Optional<String> outputString,
                       Optional<Urn> queryUrn,
                       Optional<Integer> position) {
        performanceMetricsEngine.startMeasuring(MetricType.PERFORM_SEARCH);
        deactivateSearchView();
        showResultsFor(apiQuery, userQuery, outputString, queryUrn, position);
    }

    void onSuggestionClicked() {
        deactivateSearchView();
    }

    private void setupTransitionAnimation(Window window) {
        TransitionUtils.setChangeBoundsEnterTransition(window,
                                                       TransitionUtils.ENTER_DURATION,
                                                       new DecelerateInterpolator());
        TransitionUtils.setChangeBoundsExitTransition(window,
                                                      TransitionUtils.EXIT_DURATION,
                                                      new DecelerateInterpolator());
    }

    private void setupViews(AppCompatActivity activity) {
        setupToolbar(activity);
        setupSuggestionsView();
        setupViewFlipper(activity);
        setSearchListeners();
        requestSearchFocus();
    }

    //Android Lint only sees the chained fragment transaction calls together
    //without a commit but fails to see the commit on a later row.
    @SuppressLint("CommitTransaction")
    private void setupSuggestionsView() {
        final Fragment fragment = getSuggestionFragment();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(R.id.search_suggestions_container,
                         fragment != null ? fragment : new SearchSuggestionsFragment(),
                         SearchSuggestionsFragment.TAG)
                .commit();
    }

    private SearchSuggestionsFragment getSuggestionFragment() {
        return (SearchSuggestionsFragment) fragmentManager.findFragmentByTag(SearchSuggestionsFragment.TAG);
    }

    private void setupViewFlipper(Activity activity) {
        searchViewFlipper = activity.findViewById(R.id.search_view_flipper);
        searchViewFlipper.setInAnimation(AnimationUtils.loadAnimation(activity, R.anim.activity_open_enter));
        searchViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(activity, R.anim.activity_open_exit));
    }

    private void setupToolbar(AppCompatActivity activity) {
        final Toolbar toolbar = activity.findViewById(R.id.toolbar_id);
        final ViewGroup searchView = (ViewGroup) ((LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.search_text_view, toolbar, false);
        final ActionBar actionBar = activity.getSupportActionBar();
        toolbarElevation = activity.findViewById(R.id.legacy_elevation);
        searchTextView = searchView.findViewById(R.id.search_edit_text);
        searchCloseView = searchView.findViewById(R.id.search_close);
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
        searchCloseView.setOnClickListener(new SearchCloseClickListener());
        searchTextView.setOnFocusChangeListener(new SearchFocusListener());
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
        if (!topResultsConfig.isEnabled()) {
            setElevation(searchViewIndex);
        }
        if (searchViewFlipper.getDisplayedChild() != searchViewIndex) {
            searchViewFlipper.setDisplayedChild(searchViewIndex);
        }
        if (searchViewIndex == RESULTS_VIEW_INDEX) {
            hideKeyboard();
        }
    }

    private void setElevation(int searchViewIndex) {
        if (searchViewIndex == RESULTS_VIEW_INDEX) {
            ViewCompat.setElevation(searchViewFlipper, resources.getDimension(R.dimen.toolbar_elevation));
            toolbarElevation.setVisibility(View.INVISIBLE);
        } else {
            ViewCompat.setElevation(searchViewFlipper, 0);
            toolbarElevation.setVisibility(View.VISIBLE);
        }
    }

    private void showResultsFor(String apiQuery,
                                String userQuery,
                                Optional<String> outputText,
                                Optional<Urn> queryUrn,
                                Optional<Integer> queryPosition) {
        final Fragment searchResults = getResultsFragment(apiQuery, userQuery, queryUrn, queryPosition);
        fragmentManager
                .beginTransaction()
                .replace(R.id.search_results_container, searchResults, SEARCH_RESULTS_TAG)
                .commit();
        showOutputText(outputText);
        displaySearchView(RESULTS_VIEW_INDEX);
    }

    private Fragment getResultsFragment(String apiQuery, String userQuery, Optional<Urn> queryUrn, Optional<Integer> queryPosition) {
        return topResultsConfig.isEnabled() ? TopResultsFragment.newInstance(apiQuery, userQuery, queryUrn, queryPosition)
                                            : TabbedSearchFragment.newInstance(apiQuery, userQuery, queryUrn, queryPosition);
    }

    private void showOutputText(Optional<String> outputText) {
        outputText.ifPresent(text -> {
            searchTextView.setText(text);
            searchTextView.setSelection(text.length());
        });
    }

    private void showSuggestionsFor(String query) {
        final SearchSuggestionsFragment suggestionFragment = getSuggestionFragment();
        if (suggestionFragment != null) {
            fragmentTransaction.show(suggestionFragment);
            suggestionFragment.showSuggestionsFor(query);
        }
    }

    private void showCloseButton() {
        searchCloseView.setVisibility(View.VISIBLE);
    }

    private void hideCloseButton() {
        searchCloseView.setVisibility(View.INVISIBLE);
    }

    private void hideSearchSuggestionsView() {
        fragmentTransaction.hide(getSuggestionFragment());
    }

    private void showSearchResultsView() {
        searchViewFlipper.animate().alpha(1).start();
        searchViewFlipper.setVisibility(View.VISIBLE);
    }

    private void hideSearchResultsView() {
        searchViewFlipper.animate().alpha(0).start();
        searchViewFlipper.setVisibility(View.INVISIBLE);
    }

    private void showKeyboard() {
        keyboardHelper.show(window, searchTextView);
    }

    private void hideKeyboard() {
        keyboardHelper.hide(window, searchTextView);
    }

    private void setupBackground(final AppCompatActivity activity) {
        activity.findViewById(R.id.search_screen_bg).setOnClickListener(v -> dismiss(activity));
    }

    public void dismiss(final AppCompatActivity activity) {
        if (TransitionUtils.transitionsSupported()) {
            ((ViewGroup) activity.findViewById(R.id.toolbar_id)).getChildAt(1)
                                                                .animate()
                                                                .alpha(0)
                                                                .setDuration(300)
                                                                .setListener(new AnimatorListenerAdapter() {
                                                                    @Override
                                                                    public void onAnimationEnd(Animator animation) {
                                                                        activity.supportFinishAfterTransition();
                                                                    }
                                                                }).start();
        } else {
            activity.supportFinishAfterTransition();
        }
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
                hideSearchSuggestionsView();
                hideCloseButton();
                hideSearchResultsView();
            } else {
                showCloseButton();
                showSearchResultsView();
            }
        }
    }

    private class SearchActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_SEARCH) {
                final String query = searchTextView.getText().toString();
                performSearch(query, query);
                return true;
            }
            return false;
        }
    }

    private class SearchCloseClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            eventTracker.trackSearch(SearchEvent.searchFormulationExit(screenProvider.getLastScreen(),
                                                                       searchTextView.getText().toString()));
            searchTextView.setText(Strings.EMPTY);
            hideSearchSuggestionsView();
            hideCloseButton();
            activateSearchView();
            displaySearchView(SUGGESTIONS_VIEW_INDEX);
            searchTracker.trackMainScreenEvent();
        }
    }

    private class SearchFocusListener implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (hasFocus) {
                eventTracker.trackSearch(SearchEvent.searchFormulationInit(screenProvider.getLastScreen(),
                                                                           searchTextView.getText().toString()));
            }
        }
    }
}
