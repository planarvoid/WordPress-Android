package com.soundcloud.android.search;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.LightCycle;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;

import javax.inject.Inject;

public class SearchActivity extends ScActivity implements PlaylistTagsFragment.PlaylistTagsFragmentListener {

    private static final String ACTION_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    private static final String INTENT_URL_HOST = "soundcloud.com";
    private static final String INTENT_URL_QUERY_PARAM = "q";
    private static final String INTENT_URI_SEARCH_PATH = "/search";

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle SearchActionBarController searchActionBarController;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject PlaybackInitiator playbackInitiator;

    private final SearchActionBarController.SearchCallback searchCallback = new SearchActionBarController.SearchCallback() {
        @Override
        public void performTextSearch(String query) {
            addContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
        }

        @Override
        public void performTagSearch(String tag) {
            addContent(PlaylistResultsFragment.create(tag), PlaylistResultsFragment.TAG);
        }

        @Override
        public void exitSearchMode() {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_MAIN));
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    };

    public SearchActivity() {
        searchActionBarController.setSearchCallback(searchCallback);
    }

    @VisibleForTesting
    SearchActivity(SearchActionBarController searchActionBarController) {
        this();
        this.searchActionBarController = searchActionBarController;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayoutWithMargins(this);
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
        searchActionBarController.onCreateOptionsMenu(menu, getMenuInflater(), this);
        return true;
    }

    private void addContentFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new PlaylistTagsFragment()).commit();
    }

    private void addContent(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();

        manager.beginTransaction()
                .replace(R.id.container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    public void onTagSelected(Context context, String tag) {
        if (!isFinishing()) {
            searchActionBarController.setQuery("#" + tag);
            addContent(PlaylistResultsFragment.create(tag), PlaylistResultsFragment.TAG);
        }
    }

    @Override
    public void onTagsScrolled() {
        searchActionBarController.clearFocus();
    }

    private void handleIntent() {
        addContentFragment();
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
        Uri uri = intent.getData();

        return uri != null
                && (uri.getHost().equals(INTENT_URL_HOST) || Urn.SOUNDCLOUD_SCHEME.equals(uri.getScheme()))
                && Strings.isNotBlank(uri.getQueryParameter(INTENT_URL_QUERY_PARAM));
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
