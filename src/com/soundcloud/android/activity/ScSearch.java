package com.soundcloud.android.activity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.*;
import com.soundcloud.android.model.SearchHistoryItem;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.android.view.WorkspaceView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;

import static android.widget.FrameLayout.LayoutParams.FILL_PARENT;

// XXX decouple from ScActivity
public class ScSearch extends ScActivity {
    private EditText txtQuery;

    private RadioGroup rdoType;
    private RadioButton rdoUser;
    private RadioButton rdoTrack;

    private ListView mHistoryList;

    private FrameLayout mListHolder;

    private boolean mHasHistory;

    private ScListView mList;
    private SectionedEndlessAdapter mTrackAdpWrapper;
    private SectionedEndlessAdapter mUserAdpWrapper;

    private WorkspaceView mWorkspaceView;
    private SearchHistoryAdapter mHistoryAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);



        setContentView(R.layout.workspace_view);

        mWorkspaceView = (WorkspaceView) ((ViewGroup) findViewById(android.R.id.content)).findViewById(R.id.workspace_view);
        mWorkspaceView.addView(getLayoutInflater().inflate(R.layout.sc_search_controls, null));


        rdoType = (RadioGroup) findViewById(R.id.rdo_search_type);
        rdoUser = (RadioButton) findViewById(R.id.rdo_users);
        rdoTrack = (RadioButton) findViewById(R.id.rdo_tracks);

        CloudUtils.setTextShadowForGrayBg(rdoUser);
        CloudUtils.setTextShadowForGrayBg(rdoTrack);

        txtQuery = (EditText) findViewById(R.id.query);

        Button btnSearch = (Button) findViewById(R.id.search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSearch();
            }
        });

        mList = new SectionedListView(this);
        configureList(mList);

        mListHolder = new FrameLayout(this);
        mListHolder.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        mListHolder.addView(mList);
        mList.setVisibility(View.GONE);

        mTrackAdpWrapper = new SectionedEndlessAdapter(this, new SectionedTracklistAdapter(this), true);
        mUserAdpWrapper = new SectionedEndlessAdapter(this, new SectionedUserlistAdapter(this), true);

        mList.setId(android.R.id.list);

        btnSearch.setNextFocusDownId(android.R.id.list);

        final View root = findViewById(R.id.search_root);
        root.setOnFocusChangeListener(keyboardHideFocusListener);
        root.setOnClickListener(keyboardHideClickListener);
        root.setOnTouchListener(keyboardHideTouchListener);

        txtQuery.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (!isFinishing() &&
                        keyCode == KeyEvent.KEYCODE_ENTER) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });

        mHistoryList = new ListView(this);
        mHistoryList.setSelector(R.drawable.list_selector_background);
        ((ViewGroup) findViewById(R.id.fl_searches)).addView(mHistoryList,
                new FrameLayout.LayoutParams(FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));


        mHistoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchHistoryItem item = ((SearchHistoryItem) parent.getItemAtPosition(position));
                doSearch(item.query, item.search_type, item.id);
            }
        });

        mHistoryAdapter = new SearchHistoryAdapter(this);
        mHistoryList.setAdapter(mHistoryAdapter);

        refreshHistory();

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            setListType(mPreviousState[0] != null && mPreviousState[0].equals(User.class));
            mTrackAdpWrapper.restoreState((Object[]) mPreviousState[2]);
            mUserAdpWrapper.restoreState((Object[]) mPreviousState[3]);

            if ((Integer) mPreviousState[1] == View.VISIBLE){
                mWorkspaceView.addView(mListHolder);
                mList.setVisibility(View.VISIBLE);
            }

            if ((Integer) mPreviousState[4] == 1){
                mWorkspaceView.initWorkspace(1);
            } else {
                mWorkspaceView.initWorkspace(0);
            }
        } else {
            mWorkspaceView.initWorkspace(0);
            setListType(false);
        }
    }

    private void refreshHistory() {

        Cursor cursor = getContentResolver().query(Content.SEARCHES.uri,
                new String[]{DBHelper.Searches._ID, DBHelper.Searches.SEARCH_TYPE, DBHelper.Searches.QUERY, DBHelper.Searches.CREATED_AT},
                DBHelper.Searches.USER_ID + " = ?",
                new String[]{Long.toString(getCurrentUserId())},
                DBHelper.Searches.CREATED_AT + " DESC");

        ArrayList<SearchHistoryItem> history = mHistoryAdapter.getData();
        history.clear();

        if (cursor != null && cursor.moveToFirst()) {
            mHasHistory = true;
            do {
                history.add(new SearchHistoryItem(cursor));
            } while (cursor.moveToNext());
        } else {
            mHasHistory = false;
        }
        if (cursor != null) cursor.close();

        for (SearchHistoryItem searchDefault : searchDefaults){
            if (!history.contains(searchDefault)) history.add(searchDefault);
        }

        mHistoryAdapter.notifyDataSetChanged();
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

    private void setListType(boolean isUser) {
        mList.setAdapter(isUser ? mUserAdpWrapper : mTrackAdpWrapper, true);
        mList.setLongClickable(!isUser);
        mUserAdpWrapper.configureViews(isUser ? mList : null);
        mTrackAdpWrapper.configureViews(isUser ? null : mList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        trackPage(Consts.Tracking.SEARCH);
    }

    private void doSearch() {
        doSearch(txtQuery.getText().toString(),
                (rdoType.getCheckedRadioButtonId() == R.id.rdo_tracks) ? 0 : 1, -1);
    }

    public void doSearch(String query) {
        doSearch(query, 0, -1);
    }

    void doSearch(final String query, int type, long updateId) {
        if (TextUtils.isEmpty(query)) return;

        // when called from Main or History
        txtQuery.setText(query);
        if (type == 0 && (rdoType.getCheckedRadioButtonId() != R.id.rdo_tracks)){
            rdoTrack.setChecked(true);
        } else if (type == 1 && (rdoType.getCheckedRadioButtonId() == R.id.rdo_tracks)){
            rdoUser.setChecked(true);
        }

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Searches.CREATED_AT, System.currentTimeMillis());
        cv.put(DBHelper.Searches.USER_ID, getCurrentUserId());
        cv.put(DBHelper.Searches.QUERY, query);
        int searchType;
        if (type == 0) {
            mTrackAdpWrapper.clearSections();
            mTrackAdpWrapper.addSection(new SectionedAdapter.Section(getString(R.string.list_header_track_results_for,
                    query), Track.class, new ArrayList<Parcelable>(),
                    null, //CloudUtils.replaceWildcard(Content.SEARCHES_TRACKS_ITEM.uri,query),
                    Request.to(Endpoints.TRACKS).with("q", query)
            ));

            setListType(false);
            mUserAdpWrapper.clearRefreshTask();
            mUserAdpWrapper.reset(false);
            searchType = 0;
            trackPage(Consts.Tracking.SEARCH_TRACKS + query);
        } else {
            mUserAdpWrapper.clearSections();
            mUserAdpWrapper.addSection(new SectionedAdapter.Section(getString(R.string.list_header_user_results_for,
                    query), User.class, new ArrayList<Parcelable>(),
                    null, //CloudUtils.replaceWildcard(Content.SEARCHES_USERS_ITEM.uri,query),
                    Request.to(Endpoints.USERS).with("q", query)
            ));

            setListType(true);
            mTrackAdpWrapper.clearRefreshTask();
            mTrackAdpWrapper.reset(false);
            searchType = 1;
            trackPage(Consts.Tracking.SEARCH_USERS + query);
        }

        cv.put(DBHelper.Searches.SEARCH_TYPE, searchType);


        // check for a duplicate to update
        if (updateId == -1) {
            Cursor cursor = getContentResolver().query(Content.SEARCHES.uri,
                    new String[]{DBHelper.Searches._ID},
                    DBHelper.Searches.USER_ID + " = ? AND " + DBHelper.Searches.QUERY + " = ? AND " + DBHelper.Searches.SEARCH_TYPE + " = ?",
                    new String[]{Long.toString(getCurrentUserId()), query, String.valueOf(searchType)}, null);

            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                updateId = cursor.getInt(0);
            }
            if (cursor != null) cursor.close();
        }

        if (updateId > 0) {
            getContentResolver().update(Content.SEARCHES.buildUpon().appendPath(Long.toString(updateId)).build(),
                    cv, null,null);
        } else {
            getContentResolver().insert(Content.SEARCHES.uri, cv);
        }
        refreshHistory();

        mList.setVisibility(View.VISIBLE);
        mList.setLastUpdated(0);
        mList.onRefresh(false);
        mList.setVisibility(View.VISIBLE);

        if (mWorkspaceView.getChildCount() < 2){
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
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mWorkspaceView != null && keyCode == KeyEvent.KEYCODE_BACK &&
                mWorkspaceView.getCurrentScreen() != 0) {
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


    private SearchHistoryItem[] searchDefaults = new SearchHistoryItem[] {
            new SearchHistoryItem("Interviews",0),
            new SearchHistoryItem("Bluegrass",0),
            new SearchHistoryItem("Sounds from Monday Morning",0),
            new SearchHistoryItem("Freestyle acapella",0),
            new SearchHistoryItem("Sound Effects",0),
            new SearchHistoryItem("Learn Spanish",0),
            new SearchHistoryItem("Field Recording",0),
            new SearchHistoryItem("Audio Book",0),
            new SearchHistoryItem("Guitar riff",0),
            new SearchHistoryItem("Laughing",0)
    };

}
