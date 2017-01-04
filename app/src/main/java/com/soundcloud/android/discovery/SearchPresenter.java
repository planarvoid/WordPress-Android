package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.search.suggestions.SearchSuggestionsFragment;
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

class SearchPresenter extends DefaultActivityLightCycle<AppCompatActivity>
        implements SearchIntentResolver.DeepLinkListener {

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
                    ScreenProvider screenProvider) {
        this.intentResolver = intentResolverFactory.create(this);
        this.searchTracker = searchTracker;
        this.resources = resources;
        this.eventBus = eventBus;
        this.keyboardHelper = keyboardHelper;
        this.eventTracker = eventTracker;
        this.screenProvider = screenProvider;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.window = activity.getWindow();
        this.fragmentManager = activity.getSupportFragmentManager();
        setupBackground(activity);
        setupTransitionAnimation(window);
        setupViews(activity);
        if (bundle == null) {
            intentResolver.handle(activity, activity.getIntent());
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
        performSearch(searchQuery);
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

    void onScrollChanged() {
        hideKeyboard();
    }

    void performSearch(String searchQuery) {
        performSearch(searchQuery, Optional.absent(), Optional.absent(), Optional.absent());
    }

    void performSearch(String searchQuery,
                       Optional<String> outputString,
                       Optional<Urn> queryUrn,
                       Optional<Integer> position) {
        deactivateSearchView();
        showResultsFor(searchQuery, outputString, queryUrn, position);
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
            ViewCompat.setElevation(searchViewFlipper, resources.getDimension(R.dimen.toolbar_elevation));
            toolbarElevation.setVisibility(View.INVISIBLE);
        } else {
            ViewCompat.setElevation(searchViewFlipper, 0);
            toolbarElevation.setVisibility(View.VISIBLE);
        }
    }

    private void showResultsFor(String query,
                                Optional<String> outputText,
                                Optional<Urn> queryUrn,
                                Optional<Integer> queryPosition) {
        final TabbedSearchFragment searchResults = TabbedSearchFragment.newInstance(query, queryUrn, queryPosition);
        fragmentManager
                .beginTransaction()
                .replace(R.id.search_results_container, searchResults, TabbedSearchFragment.TAG)
                .commit();
        showOutputText(outputText);
        displaySearchView(RESULTS_VIEW_INDEX);
    }

    private void showOutputText(Optional<String> outputText) {
        if (outputText.isPresent()) {
            searchTextView.setText(outputText.get());
        }
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
        activity.findViewById(R.id.search_screen_bg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(activity);
            }
        });
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
                performSearch(searchTextView.getText().toString());
                return true;
            }
            return false;
        }
    }

    private class SearchCloseClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
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
