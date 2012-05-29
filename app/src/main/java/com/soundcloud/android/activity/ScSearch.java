package com.soundcloud.android.activity;

import static android.widget.FrameLayout.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SearchHistoryAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedTracklistAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.WorkspaceView;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.List;

@Tracking(page = Page.Search_main)
public class ScSearch extends ScActivity {
    private EditText txtQuery;

    private RadioGroup rdoSearchType;
    private RadioButton rdoUser, rdoTrack;

    private FrameLayout mListHolder;
    private ScListView mList;
    SectionedEndlessAdapter mSoundAdpWrapper, mUserAdpWrapper;

    private WorkspaceView mWorkspaceView;
    private SearchHistoryAdapter mHistoryAdapter;
    private Search mCurrentSearch;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.workspace_view);

        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace_view);
        mWorkspaceView.setIgnoreChildFocusRequests(true); // we handle scrolling manually
        mWorkspaceView.addView(getLayoutInflater().inflate(R.layout.sc_search_controls, null));

        rdoSearchType = (RadioGroup) findViewById(R.id.rdo_search_type);
        rdoUser = (RadioButton) findViewById(R.id.rdo_users);
        rdoTrack = (RadioButton) findViewById(R.id.rdo_tracks);

        CloudUtils.setTextShadowForGrayBg(rdoUser);
        CloudUtils.setTextShadowForGrayBg(rdoTrack);

        txtQuery = (EditText) findViewById(R.id.query);

        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                perform(getSearch());
            }
        });

        mList = new SectionedListView(this);
        configureList(mList);

        mListHolder = new FrameLayout(this);
        mListHolder.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        mListHolder.addView(mList);
        mList.setVisibility(View.GONE);

        mSoundAdpWrapper = new SectionedEndlessAdapter(this, new SectionedTracklistAdapter(this), true);
        mUserAdpWrapper = new SectionedEndlessAdapter(this, new SectionedUserlistAdapter(this), true);

        final View root = findViewById(R.id.search_root);
        root.setOnFocusChangeListener(keyboardHideFocusListener);
        root.setOnClickListener(keyboardHideClickListener);
        root.setOnTouchListener(keyboardHideTouchListener);

        txtQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return !isFinishing() && actionId == EditorInfo.IME_ACTION_SEARCH && perform(getSearch());
            }
        });

        final ListView recentSearches = new ListView(this);
        recentSearches.setSelector(R.drawable.list_selector_background);
        ((ViewGroup) findViewById(R.id.fl_searches)).addView(recentSearches,
                new FrameLayout.LayoutParams(FILL_PARENT, FILL_PARENT));

        recentSearches.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Search search = ((Search) parent.getItemAtPosition(position));
                if (search != null) {
                    perform(search);
                }
            }
        });

        mHistoryAdapter = new SearchHistoryAdapter(this);
        recentSearches.setAdapter(mHistoryAdapter);

        Object[] previousState = getLastNonConfigurationInstance();
        if (previousState != null) {
            restorePreviousState(previousState);
        } else {
            mWorkspaceView.initWorkspace(0);
        }
    }

    @Override
    public Object[] getLastNonConfigurationInstance() {
        return (Object[]) super.getLastNonConfigurationInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHistory(getContentResolver(), mHistoryAdapter);
        track(getClass());
    }

    private Search getSearch() {
        return new Search(txtQuery.getText().toString(),
                (rdoSearchType.getCheckedRadioButtonId() == R.id.rdo_tracks) ?
                Search.SOUNDS : Search.USERS);
    }

    boolean perform(final Search search) {
        if (search.isEmpty()) return false;
        // when called from Main or History
        txtQuery.setText(search.query);

        switch (search.search_type) {
            case Search.SOUNDS:
                rdoTrack.setChecked(true);
                configureAdapter(mSoundAdpWrapper, search);
                track(Page.Search_results__sounds__keyword, search.query);
                break;

            case Search.USERS:
                rdoUser.setChecked(true);
                configureAdapter(mUserAdpWrapper, search);
                track(Page.Search_results__people__keyword, search.query);
                break;
            default:
                Log.w(TAG, "unknown search type " + search.search_type);
                return false;
        }

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        mList.setLastUpdated(0);
        mList.setVisibility(View.VISIBLE);

        if (mWorkspaceView.getChildCount() < 2) {
            // only add list after 1st search (otherwise it is scrollable on load)
            mWorkspaceView.addView(mListHolder);
        }

        if (mWorkspaceView.getCurrentScreen() != 1) {
            mWorkspaceView.scrollRight();
        }
        refreshHistory(getContentResolver(), mHistoryAdapter, search);
        mCurrentSearch = search;
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // handle back button to go back to previous screen
        if (keyCode == KeyEvent.KEYCODE_BACK
                && mWorkspaceView != null
                && mWorkspaceView.getCurrentScreen() != 0) {
            mWorkspaceView.scrollLeft();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private final View.OnFocusChangeListener keyboardHideFocusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            hideKeyboard();
        }
    };

    private final View.OnClickListener keyboardHideClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            hideKeyboard();
        }
    };

    private final View.OnTouchListener keyboardHideTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            hideKeyboard();
            return false;
        }
    };

    private void hideKeyboard() {
        if (!isFinishing()) {
            InputMethodManager mgr = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mgr != null)
                mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private SectionedEndlessAdapter configureAdapter(SectionedEndlessAdapter adapter, Search search) {
        adapter.reset();
        adapter.clearSections();
        adapter.addSection(search.getSection(this));
        adapter.configureViews(mList);
        mList.setAdapter(adapter, true);
        mList.setLongClickable(search.search_type == Search.SOUNDS);
        return adapter;
    }

    @Override
    public Object[] onRetainNonConfigurationInstance() {
        return new Object[] {
                mCurrentSearch,
                mList.getVisibility(),
                mSoundAdpWrapper.saveState(),
                mUserAdpWrapper.saveState(),
                mWorkspaceView.getCurrentScreen()
        };
    }

    void restorePreviousState(Object[] previous) {
        mCurrentSearch = (Search) previous[0];
        if ((Integer) previous[1] == View.VISIBLE) {
            mWorkspaceView.addView(mListHolder);
            mList.setVisibility(View.VISIBLE);
        }
        mSoundAdpWrapper.restoreState((Object[]) previous[2]);
        mUserAdpWrapper.restoreState((Object[]) previous[3]);
        mWorkspaceView.initWorkspace((Integer) previous[4]);
        if (mCurrentSearch != null) {
            txtQuery.setText(mCurrentSearch.query);
            configureAdapter(mCurrentSearch.search_type == Search.SOUNDS ?
                    mSoundAdpWrapper : mUserAdpWrapper, mCurrentSearch);
        }
    }

    @SuppressWarnings("unchecked")
    private static AsyncTask refreshHistory(final ContentResolver resolver,
                                            final SearchHistoryAdapter adapter,
                                            final Search... toInsert) {
        return new AsyncTask<Void, Void, List<Search>>() {
            @Override protected List<Search> doInBackground(Void... params) {
                if (toInsert != null) for (Search s : toInsert) s.insert(resolver);
                return Search.getHistory(resolver);
            }

            @Override protected void onPostExecute(List<Search> searches) {
                if (searches != null) {
                    for (Search searchDefault : SEARCH_DEFAULTS) {
                        if (!searches.contains(searchDefault)) searches.add(searchDefault);
                    }
                    adapter.setData(searches);
                }
            }
        }.execute();
    }

    private static final Search[] SEARCH_DEFAULTS = new Search[] {
            new Search("Comedy show", Search.SOUNDS),
            new Search("Bird calls", Search.SOUNDS),
            new Search("Ambient", Search.SOUNDS),
            new Search("Rap", Search.SOUNDS),
            new Search("Garage rock", Search.SOUNDS),
            new Search("Thunder storm", Search.SOUNDS),
            new Search("Snoring", Search.SOUNDS),
            new Search("Goa", Search.SOUNDS),
            new Search("Nature sounds", Search.SOUNDS),
            new Search("dubstep", Search.SOUNDS),
    };
}
