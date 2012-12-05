package com.soundcloud.android.activity;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.view.menu.ActionMenuView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestionsAdapter;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.NowPlayingIndicator;
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

import java.net.URL;

public class ActionBarController {
    @NotNull  private ActionBarOwner mOwner;
    @NotNull  private Activity mActivity;
    @Nullable private RootView mRootView;

    @NotNull private View           mActionBarCustomView;
    @NotNull private RelativeLayout mSearchCustomView;
    @NotNull private SearchView     mSearchView;

    @NotNull private NowPlayingIndicator mNowPlaying;
    @NotNull private View                mNowPlayingHolder;

    private View mMenuIndicator;
    private SuggestionsAdapter mSuggestionsAdapter;

    private boolean mInSearchMode;
    private boolean mCloseSearchOnResume;

    public interface ActionBarOwner {

        @NotNull
        public Activity     getActivity();
        public ActionBar    getSupportActionBar();
        public MenuInflater getSupportMenuInflater();
        public void         invalidateOptionsMenu();
        public int          getMenuResourceId();
    }
    public ActionBarController(@NotNull ActionBarOwner owner, @NotNull RootView rootView) {
        mOwner    = owner;
        mActivity = owner.getActivity();
        mRootView = rootView;

        View.OnClickListener toggleRootView = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRootView.animateToggleMenu();
            }
        };

        // No open the player if we're already there
        View.OnClickListener openPlayer = mActivity instanceof ScPlayer ? null : new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRootView.isExpanded()) {
                    ScActivity.startNavActivity(mActivity, ScPlayer.class, mRootView.getMenuBundle());
                } else {
                    mActivity.startActivity(new Intent(Actions.PLAYER));
                }
            }
        };

        mActionBarCustomView = createDefaultCustomView(mActivity, toggleRootView, openPlayer);
        mNowPlaying       = (NowPlayingIndicator) mActionBarCustomView.findViewById(R.id.waveform_progress);
        mNowPlayingHolder = mActionBarCustomView.findViewById(R.id.waveform_holder);

        Context themedContext = mOwner.getSupportActionBar().getThemedContext();
        mSearchCustomView     = createSearchCustomView(themedContext);
        mSearchView           = createSearchView(themedContext);
        setupSearchView(mSearchView);

        mSearchCustomView.addView(mSearchView);

        configureCustomView();
    }

    private void updateWaveformVisibility() {
        if (mActivity instanceof ScPlayer || CloudPlaybackService.getCurrentTrackId() < 0) {
            mNowPlayingHolder.setVisibility(View.GONE);
        } else {
            mNowPlayingHolder.setVisibility(View.VISIBLE);
        }
    }

    public void setTitle(CharSequence title) {
        TextView titleView = (TextView) mActionBarCustomView.findViewById(R.id.title);
        titleView.setText(title);
    }

    public void onCloseSearch() {
        if (TextUtils.isEmpty(mSearchView.getQuery())) {
            toggleSearch();
        } else {
            mSearchView.setIconified(true);
        }
    }

    public void onMenuOpenLeft() {
        if (mMenuIndicator != null) mMenuIndicator.setVisibility(View.GONE);
    }

    public void onMenuClosed() {
        if (mMenuIndicator != null && mRootView != null) mMenuIndicator.setVisibility(View.VISIBLE);
    }

    public void onResume() {
        if (!(mActivity instanceof ScPlayer)) {
            mNowPlaying.resume();
        }

        updateWaveformVisibility();

        if (mCloseSearchOnResume) {
            closeSearch(true);
            mCloseSearchOnResume = false;
        }
    }

    public void onPause() {
        if (!(mActivity instanceof ScPlayer)) {
            mNowPlaying.pause();
        }
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
            if (mRootView != null){
                mRootView.unBlock(true);
            }
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
            mOwner.getSupportActionBar().setCustomView(mInSearchMode && mRootView != null ?
                    mSearchCustomView : mActionBarCustomView, new ActionBar.LayoutParams(Gravity.FILL_HORIZONTAL)
            );
        }
    }


    private static View createDefaultCustomView(Activity activity,
                                                @Nullable View.OnClickListener homeListener,
                                                @Nullable View.OnClickListener waveformListener) {
        View defaultCustomView = View.inflate(activity, R.layout.action_bar_custom_view, null);

        defaultCustomView.findViewById(R.id.custom_home).setOnClickListener(homeListener);
        defaultCustomView.findViewById(R.id.waveform_holder).setOnClickListener(waveformListener);

        return defaultCustomView;
    }

    private static RelativeLayout createSearchCustomView(Context themedContext) {
        RelativeLayout searchCustomView = new RelativeLayout(themedContext);

        return searchCustomView;
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

           mSuggestionsAdapter = new SuggestionsAdapter(mActivity, (AndroidCloudAPI) mActivity.getApplication());
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

                   // close IME
                   mSearchView.clearFocus();

                   final Uri itemUri = mSuggestionsAdapter.getItemIntentData(position);
                   mActivity.startActivity(new Intent(Intent.ACTION_VIEW).setData(itemUri));
                   return true;
               }
           });

           // listeners for showing and hiding the content blocker
           if (mRootView != null && useFullScreenSearch()) {
               searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                   @Override
                   public boolean onQueryTextSubmit(String query) {
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
                           } else {
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
        mActivity.invalidateOptionsMenu();
    }

    public void closeSearch(boolean instant) {
        mSearchView.clearFocus();
        if (mRootView != null)mRootView.unBlock(instant);
        if (mInSearchMode) toggleSearch();
    }

    private boolean useFullScreenSearch(){
        return (mActivity.getResources().getConfiguration().screenLayout &
                                Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public void onCreateOptionsMenu(Menu menu) {
        if (mInSearchMode) {
            mOwner.getSupportMenuInflater().inflate(R.menu.search_mode, menu);
            mSearchView.setIconified(false); // this will set focus on the searchview and update the IME
        } else {
            final int menuResourceId = mOwner.getMenuResourceId();
            if (menuResourceId > 0) mOwner.getSupportMenuInflater().inflate(menuResourceId, menu);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.enter_search:
                toggleSearch();
                return true;

            case R.id.close_search:
                if (TextUtils.isEmpty(mSearchView.getQuery())) {
                    toggleSearch();
                } else {
                    mSearchView.setIconified(true);
                }
                return true;

            default:
                return false;
        }
    }

    public SearchView getSearchView() {
        return mSearchView;
    }
}
