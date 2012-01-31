package com.soundcloud.android.activity;

import static android.widget.FrameLayout.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SearchHistoryAdapter;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedTracklistAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.WorkspaceView;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;

// XXX decouple from ScActivity
public class ScSearch extends ScActivity {
    private EditText txtQuery;

    private RadioGroup rdoSearchType;
    private RadioButton rdoUser, rdoTrack;

    private FrameLayout mListHolder;
    private ScListView mList;
    SectionedEndlessAdapter mTrackAdpWrapper, mUserAdpWrapper;

    private WorkspaceView mWorkspaceView;
    private SearchHistoryAdapter mHistoryAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.workspace_view);

        mWorkspaceView = (WorkspaceView) findViewById(R.id.workspace_view);
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
        mListHolder.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        mListHolder.addView(mList);
        mList.setVisibility(View.GONE);

        mTrackAdpWrapper = new SectionedEndlessAdapter(this, new SectionedTracklistAdapter(this), true);
        mUserAdpWrapper = new SectionedEndlessAdapter(this, new SectionedUserlistAdapter(this), true);

        mList.setId(android.R.id.list);

        final View root = findViewById(R.id.search_root);
        root.setOnFocusChangeListener(keyboardHideFocusListener);
        root.setOnClickListener(keyboardHideClickListener);
        root.setOnTouchListener(keyboardHideTouchListener);

        ListView recentSearches = new ListView(this);
        recentSearches.setSelector(R.drawable.list_selector_background);
        ((ViewGroup) findViewById(R.id.fl_searches)).addView(recentSearches,
                new FrameLayout.LayoutParams(FILL_PARENT,
                        FrameLayout.LayoutParams.FILL_PARENT));

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

        refreshHistory(getContentResolver(), mHistoryAdapter);

        Object[] previousState = (Object[]) getLastNonConfigurationInstance();
        if (previousState != null) {
            restorePreviousState(previousState);
        } else {
            mWorkspaceView.initWorkspace(0);
            setListType(Search.SOUNDS);
        }
    }

    private void restorePreviousState(Object[] previous) {
        setListType(User.class.equals(previous[0]) ? Search.USERS : Search.SOUNDS);
        mTrackAdpWrapper.restoreState((Object[]) previous[2]);
        mUserAdpWrapper.restoreState((Object[]) previous[3]);

        if ((Integer) previous[1] == View.VISIBLE) {
            mWorkspaceView.addView(mListHolder);
            mList.setVisibility(View.VISIBLE);
        }

        if ((Integer) previous[4] == 1) {
            mWorkspaceView.initWorkspace(1);
        } else {
            mWorkspaceView.initWorkspace(0);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        trackPage(Consts.Tracking.SEARCH);
    }

    private Search getSearch() {
        return new Search(txtQuery.getText().toString(),
                (rdoSearchType.getCheckedRadioButtonId() == R.id.rdo_tracks) ?
                Search.SOUNDS : Search.USERS);
    }

    boolean perform(Search search) {
        if (search.isEmpty()) return false;
        // when called from Main or History
        txtQuery.setText(search.query);

        switch (search.search_type) {
            case Search.SOUNDS:
                rdoTrack.setChecked(true);

                mTrackAdpWrapper.clearSections();
                mTrackAdpWrapper.addSection(new SectionedAdapter.Section(
                        getString(R.string.list_header_track_results_for, search.query),
                        Track.class,
                        new ArrayList<Parcelable>(),
                        null,  /* uri */
                        search.request()

                ));
                setListType(search.search_type);
                mUserAdpWrapper.reset();
                trackPage(Consts.Tracking.SEARCH_TRACKS + search.query);
                break;

            case Search.USERS:
                rdoUser.setChecked(true);

                mUserAdpWrapper.clearSections();
                mUserAdpWrapper.addSection(new SectionedAdapter.Section(
                        getString(R.string.list_header_user_results_for, search.query),
                        User.class,
                        new ArrayList<Parcelable>(),
                        null,  /* uri */
                        search.request()
                ));
                setListType(search.search_type);
                mTrackAdpWrapper.reset();
                trackPage(Consts.Tracking.SEARCH_USERS + search.query);
                break;

            default:
                Log.w(TAG, "unknown search type " + search.search_type);
                return false;
        }

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        refreshHistory(getContentResolver(), mHistoryAdapter, search);

        mList.setLastUpdated(0);
        mList.setVisibility(View.VISIBLE);

        if (mWorkspaceView.getChildCount() < 2) {
            mWorkspaceView.addView(mListHolder);
        }

        Handler myHandler = new Handler();
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mWorkspaceView.getCurrentScreen() != 1) {
                    mWorkspaceView.scrollRight();
                }
            }
        }, 100);
        return true;
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

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                mList.getWrapper().getLoadModel(true),
                mList.getVisibility(),
                mTrackAdpWrapper.saveState(),
                mUserAdpWrapper.saveState(),
                mWorkspaceView.getCurrentScreen()
        };
    }

    private void setListType(int type) {
        final boolean isUser = type == Search.USERS;
        mList.setAdapter(isUser ? mUserAdpWrapper : mTrackAdpWrapper, true);
        mList.setLongClickable(!isUser);
        mUserAdpWrapper.configureViews(isUser ? mList : null);
        mTrackAdpWrapper.configureViews(isUser ? null : mList);
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
