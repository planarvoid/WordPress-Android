package com.soundcloud.android.search;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.screen.ScreenPresenter;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.Menu;

import javax.inject.Inject;

public class SearchActivity extends ScActivity implements PlaylistTagsFragment.TagEventsListener {

    private static final String ACTION_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    private static final String INTENT_URL_HOST = "soundcloud.com";
    private static final String INTENT_URL_QUERY_PARAM = "q";
    private static final String INTENT_URI_SEARCH_PATH = "/search";


    @Inject SlidingPlayerController playerController;
    @Inject AdPlayerController adPlayerController;
    @Inject SearchActionBarController searchActionBarController;
    @Inject ScreenPresenter presenter;
    @Inject PlaybackOperations playbackOperations;

    private final SearchActionBarController.SearchCallback searchCallback = new SearchActionBarController.SearchCallback() {
        @Override
        public void performTextSearch(String query) {
            addContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
        }

        @Override
        public void performTagSearch(String tag) {
            addContent(PlaylistResultsFragment.newInstance(tag), PlaylistResultsFragment.TAG);
        }

        @Override
        public void exitSearchMode() {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    };

    public SearchActivity() {
        attachLightCycle(playerController);
        attachLightCycle(adPlayerController);
        attachLightCycle(searchActionBarController);
        searchActionBarController.setSearchCallback(searchCallback);
        presenter.attach(this);
    }

    @VisibleForTesting
    SearchActivity(SearchActionBarController searchActionBarController) {
        this();
        this.searchActionBarController = searchActionBarController;
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayoutWithMargins();
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            boolean isShowingResults = getSupportFragmentManager().getBackStackEntryCount() > 0;
            if (isShowingResults) {
                searchActionBarController.clearQuery();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            handleIntent();
        }
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
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(false);
        getMenuInflater().inflate(R.menu.search, menu);

        searchActionBarController.configureSearchState(this, menu);
        return true;
    }

    private void addPlaylistTagsFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new PlaylistTagsFragment(), PlaylistTagsFragment.TAG)
                .commit();
    }

    private void addContent(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();

        manager.beginTransaction()
                .replace(R.id.container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    public void onTagSelected(String tag) {
        searchActionBarController.setQuery("#" + tag);
        addContent(PlaylistResultsFragment.newInstance(tag), PlaylistResultsFragment.TAG);
    }

    @Override
    public void onTagsScrolled() {
        searchActionBarController.clearFocus();
    }

    private void handleIntent() {
        addPlaylistTagsFragment();
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())
                || ACTION_PLAY_FROM_SEARCH.equals(intent.getAction())
                || Actions.PERFORM_SEARCH.equals(intent.getAction())) {
            showResultsFromIntent(intent.getStringExtra(SearchManager.QUERY));
        } else if (isInterceptedSearchUrl(intent)) {
            showResultsFromIntent(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals(INTENT_URI_SEARCH_PATH)) {
            handleUri(intent);
        } else {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
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
        searchActionBarController.setQuery(query);
        addContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
    }

}
