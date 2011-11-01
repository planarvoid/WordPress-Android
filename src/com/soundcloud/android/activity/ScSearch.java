package com.soundcloud.android.activity;

import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.*;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.android.view.SectionedListView;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;

// XXX decouple from ScActivity
public class ScSearch extends ScActivity {
    private EditText txtQuery;

    private RadioGroup rdoType;
    private RadioButton rdoUser;
    private RadioButton rdoTrack;

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
                doSearch(txtQuery.getText().toString());
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
                    doSearch(txtQuery.getText().toString());
                    return true;
                }
                return false;
            }
        });

        mPreviousState = (Object[]) getLastNonConfigurationInstance();
        if (mPreviousState != null) {
            setListType(mPreviousState[0].equals(User.class));
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

    private void setListType(boolean isUser){
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

    void doSearch(final String query) {
        if (TextUtils.isEmpty(query)) return;
        txtQuery.setText(query); // when called from Main

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        rdoTrack.setVisibility(View.GONE);
        rdoUser.setVisibility(View.GONE);

        if (rdoType.getCheckedRadioButtonId() == R.id.rdo_tracks) {
            mTrackAdpWrapper.clearSections();
            mTrackAdpWrapper.addSection(new SectionedAdapter.Section(String.format(getString(R.string.list_header_track_results_for,
                    query)), Track.class, new ArrayList<Parcelable>(),
                    Request.to(Endpoints.TRACKS).with("q", query)));

            setListType(false);
            mUserAdpWrapper.clearRefreshTask();
            mUserAdpWrapper.reset(false,false);
            trackPage(Consts.Tracking.SEARCH_TRACKS + query);
        } else {
            mUserAdpWrapper.clearSections();
            mUserAdpWrapper.addSection(new SectionedAdapter.Section(String.format(getString(R.string.list_header_user_results_for,
                    query)), User.class, new ArrayList<Parcelable>(),
                    Request.to(Endpoints.USERS).with("q", query)));

            setListType(true);
            mTrackAdpWrapper.clearRefreshTask();
            mTrackAdpWrapper.reset(false,false);
            trackPage(Consts.Tracking.SEARCH_USERS + query);
        }

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

    private void showControls(){
        if (!isFinishing()){
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
}
