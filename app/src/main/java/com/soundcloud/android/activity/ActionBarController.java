package com.soundcloud.android.activity;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.activity.landing.WhoToFollowActivity;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.activity.track.PlaylistDetailActivity;
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
import android.support.v7.widget.SearchView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
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

    public void onResume() {
        // nop
    }

    public interface ActionBarOwner {
        @NotNull
        public Activity     getActivity();
        public ActionBar    getSupportActionBar();
        public MenuInflater getSupportMenuInflater();
        public void         invalidateOptionsMenu();
        public int          getMenuResourceId();
    }

    public ActionBarController(@NotNull ActionBarOwner owner, AndroidCloudAPI androidCloudAPI) {
        mOwner    = owner;
        mActivity = owner.getActivity();
        mAndroidCloudAPI = androidCloudAPI;
        configureCustomView();
    }

    public void onPause() {
        /**
         * nop for now, used by
         * {@link com.soundcloud.android.activity.NowPlayingActionBarController#onPause()}
         * **/
    }

    public void onDestroy() {
        // suggestions adapter has to stop handler thread
        if (mSuggestionsAdapter != null) mSuggestionsAdapter.onDestroy();
    }

    private void configureCustomView() {
        if (mOwner.getSupportActionBar() != null) {
            final View actionBarCustomView = getActionBarCustomView();
            if (actionBarCustomView != null) {
                mOwner.getSupportActionBar().setDisplayShowCustomEnabled(true);
                mOwner.getSupportActionBar().setCustomView(actionBarCustomView, new ActionBar.LayoutParams(Gravity.RIGHT));
            } else {
                mOwner.getSupportActionBar().setDisplayShowCustomEnabled(false);
            }
        }
    }

    public void onCreateOptionsMenu(Menu menu) {
        final int menuResourceId = mOwner.getMenuResourceId();
        if (menuResourceId > 0) mOwner.getSupportMenuInflater().inflate(menuResourceId, menu);

        final MenuItem backToSetItem = menu.findItem(R.id.menu_backToSet);
        if (backToSetItem != null) {
            boolean visible = false;
            if ((mOwner.getActivity() instanceof PlayerActivity)) {
                final Uri uri = CloudPlaybackService.getPlayQueueUri();
                if (uri != null && Content.match(uri) == Content.PLAYLIST) {
                    visible = true;
                }
            }
            backToSetItem.setVisible(visible);
        }


        SupportMenuItem searchItem = (SupportMenuItem) menu.findItem(R.id.action_search);
        if (searchItem != null){
            setupSearchView(searchItem);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_backToSet:
                final Intent intent = new Intent(mOwner.getActivity(), PlaylistDetailActivity.class);
                intent.putExtra(PlaylistDetailActivity.EXTRA_SCROLL_TO_PLAYING_TRACK, true);
                final Uri uri = CloudPlaybackService.getPlayQueueUri();
                if (Content.match(uri) == Content.PLAYLIST) {
                    intent.setData(uri);
                } else {
                    return false;
                }
                mOwner.getActivity().startActivity(intent);
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

    protected View getActionBarCustomView() {
        if (mActionBarCustomView == null) mActionBarCustomView = createCustomView();
        return mActionBarCustomView;
    }

    protected View createCustomView() {
        return null;
    }

    /**
     * Configure search view to function how we want it
     */
    private void setupSearchView(final SupportMenuItem searchItem) {

        // When using the support library, the setOnActionExpandListener() method is
        // static and accepts the MenuItem object as an argument
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mOwner.getSupportActionBar().setDisplayShowCustomEnabled(false);
                return false;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mOwner.getSupportActionBar().setDisplayShowCustomEnabled(true);
                return false;
            }
        });

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(mActivity.getComponentName());
        searchView.setSearchableInfo(searchableInfo);

        mSuggestionsAdapter = new SuggestionsAdapter(mActivity, mAndroidCloudAPI);
        searchView.setSuggestionsAdapter(mSuggestionsAdapter);
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
            searchPlate.setBackgroundResource(R.drawable.edit_text_holo_dark);

            // not found
//            searchField = SearchView.class.getDeclaredField("mCloseButon");
//            searchField.setAccessible(true);
//            ImageView closeButton = (ImageView)searchField.get(searchView);
//            closeButton.setBackgroundResource(R.drawable.action_item_background_selector);

        }
        catch (NoSuchFieldException e)
        {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            Log.e(getClass().getSimpleName(),e.getMessage(),e);
        }
    }
}
