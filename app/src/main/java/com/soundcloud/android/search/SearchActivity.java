package com.soundcloud.android.search;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.Subscriptions;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;


public class SearchActivity extends ScActivity implements PlaylistTagsFragment.TagEventsListener {

    private static final String ACTION_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    private static final String INTENT_URL_HOST = "soundcloud.com";
    private static final String INTENT_URL_QUERY_PARAM = "q";
    private static final String INTENT_URI_SEARCH_PATH = "/search";

    private static final String STATE_QUERY = "query";

    private SearchActionBarController searchActionBarController;
    private String query;

    private BehaviorSubject<Search> performSearchObservable;
    private Subscription performSearchSubscription = Subscriptions.empty();

    private final SearchActionBarController.SearchCallback searchCallback = new SearchActionBarController.SearchCallback() {
        @Override
        public void performTextSearch(String query) {
            performSearchObservable.onNext(new Search(query, Search.TYPE_TEXT));
        }

        @Override
        public void performTagSearch(String tag) {
            performSearchObservable.onNext(new Search(tag, Search.TYPE_TAG));
        }

        @Override
        public void exitSearchMode() {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_MAIN.get());
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    };

    @SuppressWarnings("unused")
    public SearchActivity() { }

    @VisibleForTesting
    SearchActivity(SearchActionBarController searchActionBarController) {
        this.searchActionBarController = searchActionBarController;
    }

    @Override
    public void onBackPressed() {
        boolean isShowingResults = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (isShowingResults) {
            searchActionBarController.clearQuery();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);

        performSearchObservable = BehaviorSubject.create();

        if (savedInstanceState == null) {
            handleIntent();
        } else if (savedInstanceState.containsKey(STATE_QUERY)) {
            query = savedInstanceState.getString(STATE_QUERY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        performSearchSubscription = performSearchObservable.subscribe(new SearchSubscriber());
    }

    @Override
    protected void onPause() {
        super.onPause();
        performSearchSubscription.unsubscribe();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (ScTextUtils.isNotBlank(query)) {
            searchActionBarController.setQuery(query);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_QUERY, searchActionBarController.getQuery());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected ActionBarController createActionBarController() {
        if (searchActionBarController == null) {
            searchActionBarController = new SearchActionBarController(this, new PublicApi(this), searchCallback, eventBus);
        }
        return searchActionBarController;
    }

    private void addPlaylistTagsFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.holder, new PlaylistTagsFragment(), PlaylistTagsFragment.TAG)
                .commit();
    }

    private void addContent(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();

        manager.beginTransaction()
                .replace(R.id.holder, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.search;
    }

    @Override
    public void onTagSelected(String tag) {
        searchActionBarController.setQuery("#" + tag);
        performSearchObservable.onNext(new Search(tag, Search.TYPE_TAG));
    }

    @Override
    public void onTagsScrolled() {
        searchActionBarController.clearFocus();
    }

    private void handleIntent() {
        addPlaylistTagsFragment();
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()) || ACTION_PLAY_FROM_SEARCH.equals(intent.getAction())) {
            showResultsFromIntent(intent.getStringExtra(SearchManager.QUERY));
        } else if (isInterceptedSearchUrl(intent)) {
            showResultsFromIntent(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals(INTENT_URI_SEARCH_PATH)) {
            handleUri(intent);
        } else {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_MAIN.get());
        }
    }

    private boolean isInterceptedSearchUrl(Intent intent) {
        return intent.getData() != null
                && intent.getData().getHost().equals(INTENT_URL_HOST)
                && ScTextUtils.isNotBlank(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
    }

    private void handleUri(final Intent intent) {
        final Content content = Content.match(intent.getData());
        if (content == Content.SEARCH_ITEM) {
            showResultsFromIntent(Uri.decode(intent.getData().getLastPathSegment()));
        } else if (content != Content.UNKNOWN) {
            // Quick search box - Resolve through normal system
            startActivity(new Intent(Intent.ACTION_VIEW).setData(intent.getData()));
            finish();
        }
    }

    private void showResultsFromIntent(String query) {
        this.query = query;
        performSearchObservable.onNext(new Search(query, Search.TYPE_TEXT));
    }

    private class SearchSubscriber extends DefaultSubscriber<Search> {
        @Override
        public void onNext(Search search) {
            if (search.type == Search.TYPE_TEXT) {
                addContent(TabbedSearchFragment.newInstance(search.query), TabbedSearchFragment.TAG);
            } else if (search.type == Search.TYPE_TAG) {
                addContent(PlaylistResultsFragment.newInstance(search.query), PlaylistResultsFragment.TAG);
            }
        }
    }

    private static class Search {
        private static final int TYPE_TEXT = 0;
        private static final int TYPE_TAG = 1;

        private String query;
        private int type;

        private Search(String query, int type) {
            this.query = query;
            this.type = type;
        }
    }

}
