package com.soundcloud.android.actionbar;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;

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

import java.lang.reflect.Field;

public class ActionBarController {
    @NotNull
    protected ActionBarOwner mOwner;
    @NotNull
    protected Activity mActivity;

    private SuggestionsAdapter mSuggestionsAdapter;
    private final PublicCloudAPI mPublicCloudAPI;
    private boolean mInSearchMode;

    private SearchView mSearchView;

    public interface ActionBarOwner {

        @NotNull
        public ActionBarActivity getActivity();
        public int getMenuResourceId();
        public boolean restoreActionBar();
    }

    public ActionBarController(@NotNull ActionBarOwner owner, PublicCloudAPI publicCloudAPI) {
        mOwner = owner;
        mActivity = owner.getActivity();
        mPublicCloudAPI = publicCloudAPI;
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
     *
     * @param menu
     */
    public void onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = mOwner.getActivity().getSupportActionBar();
        if (mInSearchMode) {
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
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), SettingsActivity.class));
                return true;

            case R.id.action_record:
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), RecordActivity.class));
                return true;

            case R.id.action_who_to_follow:
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), WhoToFollowActivity.class));
                return true;

            case R.id.action_activity:
                mOwner.getActivity().startActivity(new Intent(mOwner.getActivity(), ActivitiesActivity.class));
                return true;

            default:
                return false;
        }
    }

    private void configureToSearchState(Menu menu, ActionBar actionBar) {
        actionBar.setDisplayShowCustomEnabled(false);
        mOwner.getActivity().getMenuInflater().inflate(R.menu.search, menu);

        SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(mActivity.getComponentName());

        SupportMenuItem searchItem = (SupportMenuItem) menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setSearchableInfo(searchableInfo);
        mSearchView.setIconified(false);

        mSuggestionsAdapter = new SuggestionsAdapter(mActivity, mPublicCloudAPI);
        mSearchView.setSuggestionsAdapter(mSuggestionsAdapter);
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                closeSearch();
                return false;
            }
        });
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // we have to check against the current search field because we
                // may have old search views still providing focus changes
                if (!hasFocus && mSearchView == v) {
                    closeSearch();
                }
            }
        });
        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                mSearchView.clearFocus();
                mSearchView.setQuery("", false);
                mSearchView.setIconified(true);

                final Uri itemUri = mSuggestionsAdapter.getItemIntentData(position);
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW).setData(itemUri));
                return true;
            }
        });

        styleSearchView(mSearchView);
    }

    protected void setActionBarDefaultOptions(ActionBar actionBar) {
        if (!mOwner.restoreActionBar()) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setLogo(R.drawable.actionbar_logo);
        }
    }

    private void closeSearch(){
        if (mInSearchMode){
            if (mSearchView != null){
                mSearchView.setOnQueryTextFocusChangeListener(null);
            }
            toggleSearchMode();
        }
    }

    private void toggleSearchMode() {
        mInSearchMode = !mInSearchMode;
        mOwner.getActivity().supportInvalidateOptionsMenu();
    }

    private void styleSearchView(SearchView searchView) {
        try {
            Field searchField = SearchView.class.getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchBtn = (ImageView) searchField.get(searchView);
            searchBtn.setBackgroundResource(R.drawable.item_background_dark);

            searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeButton = (ImageView) searchField.get(searchView);
            closeButton.setBackgroundResource(R.drawable.item_background_dark);

        } catch (NoSuchFieldException e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
