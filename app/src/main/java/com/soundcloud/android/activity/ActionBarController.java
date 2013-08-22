package com.soundcloud.android.activity;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.view.menu.ActionMenuView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.track.PlaylistActivity;
import com.soundcloud.android.adapter.SuggestionsAdapter;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.RootView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ActionBarController {
    @NotNull protected ActionBarOwner mOwner;
    @NotNull protected Activity mActivity;
    @NotNull protected RootView mRootView;

    @Nullable private View              mActionBarCustomView;
    @Nullable private RelativeLayout    mSearchCustomView;
    @Nullable private SearchView        mSearchView;
    @Nullable private View              mMenuIndicator;

    private SuggestionsAdapter mSuggestionsAdapter;
    private final AndroidCloudAPI mAndroidCloudAPI;

    private boolean mInSearchMode;
    private boolean mCloseSearchOnResume;

    public interface ActionBarOwner {
        @NotNull
        public Activity     getActivity();
        public ActionBar    getSupportActionBar();
        public MenuInflater getSupportMenuInflater();
        public void         invalidateOptionsMenu();
        public int          getMenuResourceId();
        public void         onHomePressed();
    }

    public ActionBarController(@NotNull ActionBarOwner owner, @NotNull RootView rootView, AndroidCloudAPI androidCloudAPI) {
        mOwner    = owner;
        mActivity = owner.getActivity();
        mRootView = rootView;
        mAndroidCloudAPI = androidCloudAPI;
        configureCustomView();
    }


    public void setTitle(CharSequence title) {
        ((TextView) getActionBarCustomView().findViewById(R.id.title)).setText(title);
    }

    public void hideMenuIndicator() {
        getMenuIndicator().setVisibility(View.GONE);
    }

    public void showMenuIndicator() {
        getMenuIndicator().setVisibility(View.VISIBLE);
    }

    public void onResume() {
        if (mCloseSearchOnResume) {
            closeSearch(true);
            mCloseSearchOnResume = false;
        }
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

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("inSearchMode", mInSearchMode);
        savedInstanceState.putBoolean("closeSearchOnResume", mCloseSearchOnResume);
        final CharSequence query = getSearchView().getQuery();
        if (!TextUtils.isEmpty(query)) savedInstanceState.putCharSequence("searchQuery", query);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean("closeSearchOnResume")) {
            mRootView.unBlock(true);
        } else {
            if (savedInstanceState.getBoolean("inSearchMode") != mInSearchMode) {
                toggleSearch();
            }
            if (savedInstanceState.containsKey("searchQuery")) {
                getSearchView().setQuery(savedInstanceState.getCharSequence("searchQuery"), false);
                if (mInSearchMode) getSearchView().setIconified(false); // request focus
            }
        }
    }



    private void configureCustomView(){
        if (mOwner.getSupportActionBar() != null) {
            mOwner.getSupportActionBar().setCustomView(mInSearchMode ?
                    getSearchCustomView() : getActionBarCustomView(), new ActionBar.LayoutParams(Gravity.FILL_HORIZONTAL)
            );
        }
    }

    private static SearchView createSearchView(Context themedContext) {
        SearchView searchView = new SearchView(themedContext);
        searchView.setLayoutParams(new ActionMenuView.LayoutParams(ActionMenuView.LayoutParams.WRAP_CONTENT, ActionMenuView.LayoutParams.MATCH_PARENT));
        searchView.setGravity(Gravity.LEFT);
        return searchView;
    }

    /**
     * Configure search view to function how we want it
     * @param searchView the search view
     */
    private void setupSearchView(SearchView searchView) {
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && !mCloseSearchOnResume) closeSearch(false);
            }
        });

        /* find and configure the search autocompletetextview */
        // actionbarsherlock view
        final AutoCompleteTextView search_text = (AutoCompleteTextView) searchView.findViewById(R.id.abs__search_src_text);
        if (search_text != null) {
            if (useFullScreenSearch()) {
                // on a normal size device, use the whole action bar
                final int identifier = mActivity.getResources().getIdentifier("action_bar_container", "id", "android");
                if (mActivity.findViewById(identifier) != null) {
                    // native action bar (>= Honeycomb)
                    search_text.setDropDownAnchor(identifier);
                } else if (mActivity.findViewById(R.id.abs__action_bar_container) != null) {
                    // abs action bar (< Honeycomb)
                    search_text.setDropDownAnchor(R.id.abs__action_bar_container);
                }
                search_text.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);

            } else {
                // on a large screen device, just anchor to the search bar itself
                if (mActivity.findViewById(R.id.abs__search_bar) != null) search_text.setDropDownAnchor(R.id.abs__search_bar);
                search_text.setDropDownWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }

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
                // don't do the whole unblocking animation until after exit
                mCloseSearchOnResume = true;

                // close IME and kill search text
                getSearchView().clearFocus();
                if (search_text != null) search_text.setText("");

                final Uri itemUri = mSuggestionsAdapter.getItemIntentData(position);
                mActivity.startActivity(new Intent(Intent.ACTION_VIEW).setData(itemUri));
                return true;
            }
        });

        // listeners for showing and hiding the content blocker
        if (useFullScreenSearch()) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // don't do the whole unblocking animation until after exit
                    mCloseSearchOnResume = true;

                    // close IME and kill search text
                    getSearchView().clearFocus();
                    if (search_text != null) search_text.setText("");
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (TextUtils.isEmpty(newText) && !mCloseSearchOnResume) mRootView.unBlock(false);
                    return false;
                }
            });
            if (search_text != null) {
                mSuggestionsAdapter.registerDataSetObserver(new DataSetObserver() {

                    @Override
                    public void onChanged() {
                        if (mSuggestionsAdapter.getCount() > 0 && !TextUtils.isEmpty(search_text.getText())) {
                            mRootView.block();
                        } else if (!mCloseSearchOnResume) {
                            mRootView.unBlock(false);
                        }
                    }
                });
            }
        }
    }

    /**
     * Search Handling
     */

    private void toggleSearch() {
        mInSearchMode = !mInSearchMode;
        configureCustomView();
        mOwner.invalidateOptionsMenu();
    }

    public void closeSearch(boolean instant) {
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        mRootView.unBlock(instant);
        if (mInSearchMode) toggleSearch();
    }

    private boolean useFullScreenSearch(){
        return (mActivity.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public void onCreateOptionsMenu(Menu menu) {
        if (mInSearchMode) {
            mOwner.getSupportMenuInflater().inflate(R.menu.search_mode, menu);
            getSearchView().setIconified(false); // this will set focus on the searchview and update the IME
        } else {
            final int menuResourceId = mOwner.getMenuResourceId();
            if (menuResourceId > 0) mOwner.getSupportMenuInflater().inflate(menuResourceId, menu);
        }

        final MenuItem backToSetItem = menu.findItem(R.id.menu_backToSet);
        if (backToSetItem != null) {
            boolean visible = false;
            if ((mOwner.getActivity() instanceof ScPlayer)) {
                final Uri uri = CloudPlaybackService.getUri();
                if (uri != null && Content.match(uri) == Content.PLAYLIST) {
                    visible = true;
                }
            }
            backToSetItem.setVisible(visible);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_enter_search:
                toggleSearch();
                return true;

            case R.id.menu_close_search:
                if (TextUtils.isEmpty(getSearchView().getQuery())) {
                    toggleSearch();
                } else {
                    getSearchView().setIconified(true);
                }
                return true;

            case R.id.menu_backToSet:
                final Intent intent = new Intent(mOwner.getActivity(), PlaylistActivity.class);
                intent.putExtra(PlaylistActivity.EXTRA_SCROLL_TO_PLAYING_TRACK, true);
                final Uri uri = CloudPlaybackService.getUri();
                if (Content.match(uri) == Content.PLAYLIST) {
                    intent.setData(uri);
                } else {
                    return false;
                }
                mOwner.getActivity().startActivity(intent);
                return true;

            default:
                return false;
        }
    }

    @NotNull
    public View getActionBarCustomView() {
        if (mActionBarCustomView == null) mActionBarCustomView = createCustomView();
        return mActionBarCustomView;
    }

    protected View createCustomView() {
        View customView = View.inflate(mActivity, R.layout.action_bar_custom_view, null);
        setupHomeButton(customView.findViewById(R.id.custom_home));
        return customView;
    }

    protected void setupHomeButton(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOwner.onHomePressed();
            }
        });
    }

    @NotNull
    public View getMenuIndicator() {
        if (mMenuIndicator == null) {
            mMenuIndicator = getActionBarCustomView().findViewById(R.id.custom_up);
        }
        return mMenuIndicator;
    }

    @NotNull
    public RelativeLayout getSearchCustomView() {
        if (mSearchCustomView == null) {
            mSearchCustomView = new RelativeLayout(mOwner.getSupportActionBar().getThemedContext());
        }

        return mSearchCustomView;
    }

    @NotNull
    public SearchView getSearchView() {
        if (mSearchView == null) {
            final Context themedContext = mOwner.getSupportActionBar().getThemedContext();
            mSearchView = createSearchView(themedContext);
            setupSearchView(mSearchView);
            getSearchCustomView().addView(mSearchView);
        }

        return mSearchView;
    }
}
