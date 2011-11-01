package com.soundcloud.android.activity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SectionedAdapter;
import com.soundcloud.android.adapter.SectionedEndlessAdapter;
import com.soundcloud.android.adapter.SectionedTracklistAdapter;
import com.soundcloud.android.adapter.SectionedUserlistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
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
    private Cursor mHistoryCursor;

    private ScListView mList;
    private SectionedEndlessAdapter mTrackAdpWrapper;
    private SectionedEndlessAdapter mUserAdpWrapper;

    private ViewFlipper mSearchFlipper;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.sc_search);

        mSearchFlipper = (ViewFlipper) findViewById(R.id.vf_search);
        rdoType = (RadioGroup) findViewById(R.id.rdo_search_type);
        rdoUser = (RadioButton) findViewById(R.id.rdo_users);
        rdoTrack = (RadioButton) findViewById(R.id.rdo_tracks);

        txtQuery = (EditText) findViewById(R.id.query);

        Button btnSearch = (Button) findViewById(R.id.search);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSearch();
            }
        });

        rdoTrack.setVisibility(View.GONE);
        rdoUser.setVisibility(View.GONE);

        mList = new SectionedListView(this);
        configureList(mList);

        ((ViewGroup) findViewById(R.id.list_holder)).addView(mList);
        mList.setVisibility(View.GONE);

        mTrackAdpWrapper = new SectionedEndlessAdapter(this, new SectionedTracklistAdapter(this), true);
        mUserAdpWrapper = new SectionedEndlessAdapter(this, new SectionedUserlistAdapter(this), true);

        // set the list to tracks by default
        mList.setId(android.R.id.list);

        btnSearch.setNextFocusDownId(android.R.id.list);

        final View root = findViewById(R.id.search_root);
        root.setOnFocusChangeListener(keyboardHideFocusListener);
        root.setOnClickListener(keyboardHideClickListener);
        root.setOnTouchListener(keyboardHideTouchListener);

        txtQuery.setOnFocusChangeListener(queryFocusListener);
        txtQuery.setOnClickListener(queryClickListener);
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

        mHistoryCursor = getContentResolver().query(DatabaseHelper.Content.SEARCHES,
                new String[]{DatabaseHelper.Searches.ID, DatabaseHelper.Searches.SEARCH_TYPE, DatabaseHelper.Searches.QUERY, DatabaseHelper.Searches.CREATED_AT},
                DatabaseHelper.Searches.USER_ID + " = ?",
                new String[]{Long.toString(getCurrentUserId())},
                DatabaseHelper.Searches.CREATED_AT + " DESC");
        startManagingCursor(mHistoryCursor);

        SimpleCursorAdapter adp = new SimpleCursorAdapter(this, R.layout.search_history_row, mHistoryCursor,
                new String[]{DatabaseHelper.Searches.SEARCH_TYPE, DatabaseHelper.Searches.QUERY, DatabaseHelper.Searches.CREATED_AT},
                new int[]{R.id.iv_search_type, R.id.tv_query, R.id.tv_created_at});
        adp.setViewBinder(new SearchHistoryBinder());
        mHistoryList.setAdapter(adp);

        mHistoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = ((Cursor) parent.getItemAtPosition(position));
                doSearch(c.getString(c.getColumnIndex(DatabaseHelper.Searches.QUERY)),
                        c.getInt(c.getColumnIndex(DatabaseHelper.Searches.SEARCH_TYPE)),
                        c.getLong(c.getColumnIndex(DatabaseHelper.Searches.ID)));
            }
        });

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            setListType(mPreviousState[0] != null && mPreviousState[0].equals(User.class));
            mList.setVisibility(Integer.parseInt(mPreviousState[1].toString()));
            mTrackAdpWrapper.restoreState((Object[]) mPreviousState[2]);
            mUserAdpWrapper.restoreState((Object[]) mPreviousState[3]);
            mSearchFlipper.setDisplayedChild((Integer) mPreviousState[4]);
        } else {
            setListType(false);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                mList.getWrapper().getLoadModel(true),
                mList.getVisibility(),
                mTrackAdpWrapper.saveState(),
                mUserAdpWrapper.saveState(),
                mSearchFlipper.getDisplayedChild()
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
        doSearch(query,0, -1);
    }

    void doSearch(final String query, int type, long updateId) {
        if (TextUtils.isEmpty(query)) return;
        txtQuery.setText(query); // when called from Main

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        rdoTrack.setVisibility(View.GONE);
        rdoUser.setVisibility(View.GONE);

        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.Searches.CREATED_AT, System.currentTimeMillis());
        cv.put(DatabaseHelper.Searches.USER_ID, getCurrentUserId());
        cv.put(DatabaseHelper.Searches.QUERY, query);
        int searchType;
        if (type == 0) {
            mTrackAdpWrapper.clearSections();
            mTrackAdpWrapper.addSection(new SectionedAdapter.Section(String.format(getString(R.string.list_header_track_results_for,
                    query)), Track.class, new ArrayList<Parcelable>(),
                    Request.to(Endpoints.TRACKS).with("q", query)));

            setListType(false);
            mUserAdpWrapper.clearRefreshTask();
            mUserAdpWrapper.reset(false, false);
            searchType = 0;
            trackPage(Consts.Tracking.SEARCH_TRACKS + query);
        } else {
            mUserAdpWrapper.clearSections();
            mUserAdpWrapper.addSection(new SectionedAdapter.Section(String.format(getString(R.string.list_header_user_results_for,
                    query)), User.class, new ArrayList<Parcelable>(),
                    Request.to(Endpoints.USERS).with("q", query)));

            setListType(true);
            mTrackAdpWrapper.clearRefreshTask();
            mTrackAdpWrapper.reset(false, false);
            searchType = 1;
            trackPage(Consts.Tracking.SEARCH_USERS + query);
        }

        cv.put(DatabaseHelper.Searches.SEARCH_TYPE, searchType);


        // check for a duplicate to update
        if (updateId == -1){
            Cursor cursor = getContentResolver().query(DatabaseHelper.Content.SEARCHES,
                new String[]{DatabaseHelper.Searches.ID},
                DatabaseHelper.Searches.USER_ID + " = ? AND " + DatabaseHelper.Searches.QUERY + " = ? AND " + DatabaseHelper.Searches.SEARCH_TYPE +" = ?",
                new String[]{Long.toString(getCurrentUserId()), query,String.valueOf(searchType)},null);

            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                updateId = cursor.getInt(0);
            }
            if (cursor != null) cursor.close();
        }

        if (updateId > 0){
            getContentResolver().update(DatabaseHelper.Searches.CONTENT_URI, cv,DatabaseHelper.Searches.ID + " = ?",
                new String[]{Long.toString(updateId)});
        } else {
            getContentResolver().insert(DatabaseHelper.Searches.CONTENT_URI, cv);
        }
        mHistoryCursor.requery();

        mList.setLastUpdated(0);
        mList.onRefresh();
        mList.setVisibility(View.VISIBLE);

        if (mSearchFlipper.getDisplayedChild() == 0) {
            mSearchFlipper.setInAnimation(AnimUtils.inFromRightAnimation(new AccelerateDecelerateInterpolator()));
            mSearchFlipper.setOutAnimation(AnimUtils.outToLeftAnimation(new AccelerateDecelerateInterpolator()));
            mSearchFlipper.showNext();
        }
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mSearchFlipper != null && keyCode == KeyEvent.KEYCODE_BACK &&
                mSearchFlipper.getDisplayedChild() != 0) {
            mSearchFlipper.setInAnimation(AnimUtils.inFromLeftAnimation(new AccelerateDecelerateInterpolator()));
            mSearchFlipper.setOutAnimation(AnimUtils.outToRightAnimation(new AccelerateDecelerateInterpolator()));
            mSearchFlipper.showPrevious();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private View.OnFocusChangeListener queryFocusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (!isFinishing() && hasFocus) {
                showControls();
            }
        }
    };

    private View.OnClickListener queryClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            showControls();
        }
    };

    private void showControls() {
        if (!isFinishing()) {
            rdoTrack.setVisibility(View.VISIBLE);
            rdoUser.setVisibility(View.VISIBLE);
        }
    }

    private View.OnFocusChangeListener keyboardHideFocusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            hideControls();
        }
    };

    private View.OnClickListener keyboardHideClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            hideControls();
        }
    };

    private View.OnTouchListener keyboardHideTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            hideControls();
            return false;
        }
    };

    private void hideControls() {
        if (!isFinishing()) {
            InputMethodManager mgr = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mgr != null)
                mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            rdoTrack.setVisibility(View.GONE);
            rdoUser.setVisibility(View.GONE);
        }
    }

    private void setListAdapterFromSearchType(int searchType) {
        switch (searchType) {
            case 0:
                setListType(false);
                break;
            case 1:
                setListType(true);
                break;
        }
    }

    private class SearchHistoryBinder implements SimpleCursorAdapter.ViewBinder {

        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int viewId = view.getId();
            switch (viewId) {
                case R.id.tv_query:
                    ((TextView) view).setText(cursor.getString(columnIndex));
                    break;
                case R.id.tv_created_at:
                    ((TextView) view).setText(CloudUtils.getTimeElapsed(ScSearch.this.getResources(), cursor.getLong(columnIndex)));
                    break;
                case R.id.iv_search_type:
                    switch (cursor.getInt(columnIndex)) {
                        case 0:
                            ((ImageView) view).setImageResource(R.drawable.ic_user_tab_sounds);
                            break;
                        case 1:
                            ((ImageView) view).setImageResource(R.drawable.ic_profile_states);
                            break;
                    }
                    break;
            }
            return true;
        }
    }


}
