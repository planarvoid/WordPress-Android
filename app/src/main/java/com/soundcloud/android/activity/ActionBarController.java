package com.soundcloud.android.activity;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.activity.landing.WhoToFollowActivity;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.adapter.SuggestionsAdapter;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Field;

public class ActionBarController {
    @NotNull protected ActionBarOwner mOwner;
    @NotNull protected Activity mActivity;

    @Nullable private View              mActionBarCustomView;

    private SuggestionsAdapter mSuggestionsAdapter;
    private final AndroidCloudAPI mAndroidCloudAPI;
    private boolean mInSearchMode;

    public interface ActionBarOwner {

        @NotNull
        public ActionBarActivity getActivity();
        public int          getMenuResourceId();
        public boolean      restoreActionBar();
    }
    public ActionBarController(@NotNull ActionBarOwner owner, AndroidCloudAPI androidCloudAPI) {
        mOwner    = owner;
        mActivity = owner.getActivity();
        mAndroidCloudAPI = androidCloudAPI;
    }

    public void onResume() {
        /** nop for now, used by {@link NowPlayingActionBarController#onResume()} ()} **/
    }

    public void onPause() {
        /** nop for now, used by {@link NowPlayingActionBarController#onPause()} ()} **/
    }

    public void onDestroy() {
        // suggestions adapter has to stop handler thread
        if (mSuggestionsAdapter != null) mSuggestionsAdapter.onDestroy();
    }

    /**
     * This must be passed through by the activity in order to configure based on search state
     * @param menu
     */
    public void onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = mOwner.getActivity().getSupportActionBar();
        if (mInSearchMode){
            configureToSearchState(menu, actionBar);

        } else {
            setActionBarDefaultOptions(actionBar);
            final int menuResourceId = mOwner.getMenuResourceId();
            if (menuResourceId > 0) mOwner.getActivity().getMenuInflater().inflate(menuResourceId, menu);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enter_search:
                toggleSearchMode();
                return true;

            case R.id.action_settings:
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), Settings.class));
                return true;

            case R.id.action_record:
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), ScCreate.class));
                return true;

            case R.id.action_who_to_follow:
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), WhoToFollowActivity.class));
                return true;

            case R.id.action_activity:
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), News.class));
                return true;

            default:
                return false;
        }
    }

    private void configureToSearchState(Menu menu, ActionBar actionBar) {
        actionBar.setDisplayShowCustomEnabled(false);
        mOwner.getActivity().getMenuInflater().inflate(R.menu.search, menu);

        SupportMenuItem searchItem = (SupportMenuItem) menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(mActivity.getComponentName());
        searchView.setSearchableInfo(searchableInfo);

        mSuggestionsAdapter = new SuggestionsAdapter(mActivity, mAndroidCloudAPI);
        searchView.setIconified(false);
        searchView.setSuggestionsAdapter(mSuggestionsAdapter);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                toggleSearchMode();
                return false;
            }
        });
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                searchView.clearFocus();
                searchView.setQuery("", false);
                searchView.setIconified(true);

                final Uri itemUri = mSuggestionsAdapter.getItemIntentData(position);
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW).setData(itemUri));
                return true;
            }
        });

        styleSearchView(searchView);
    }

    protected void setActionBarDefaultOptions(ActionBar actionBar) {
        if (!mOwner.restoreActionBar()){
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    private void configureBackToPlaylistItem(MenuItem backToSetItem) {
        boolean visible = false;
        if ((mOwner.getActivity() instanceof PlayerActivity)) {
            final Uri uri = CloudPlaybackService.getPlayQueueUri();
            if (uri != null && Content.match(uri) == Content.PLAYLIST) {
                visible = true;
            }
        }
        backToSetItem.setVisible(visible);
    }

    private void toggleSearchMode() {
        mInSearchMode = !mInSearchMode;
        mOwner.getActivity().supportInvalidateOptionsMenu();
    }

    private void styleSearchView(SearchView searchView) {
        try
        {
            Field searchField = SearchView.class.getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchBtn = (ImageView)searchField.get(searchView);
            searchBtn.setBackgroundResource(R.drawable.action_item_background_selector);

            searchField = SearchView.class.getDeclaredField("mSearchPlate");
            searchField.setAccessible(true);
            LinearLayout searchPlate = (LinearLayout)searchField.get(searchView);
            searchPlate.setBackgroundResource(R.drawable.abc_textfield_search_default_holo_dark);

            searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeButton = (ImageView)searchField.get(searchView);
            closeButton.setBackgroundResource(R.drawable.action_item_background_selector);

        }
        catch (NoSuchFieldException e)
        {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            Log.e(getClass().getSimpleName(), e.getMessage(),e);
        }
    }
}
