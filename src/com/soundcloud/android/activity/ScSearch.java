package com.soundcloud.android.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.view.LazyList;

import java.net.URLEncoder;
import java.util.ArrayList;

public class ScSearch extends ScActivity {

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "ScSearch";

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //


    private Button btnSearch;

    private EditText txtQuery;

    private RadioGroup rdoType;

    private RadioButton rdoUser;

    private RadioButton rdoTrack;

    private LazyList mList;

    private LazyEndlessAdapter mTrackAdpWrapper;

    private LazyEndlessAdapter mUserAdpWrapper;

    private int MIN_LENGTH = 2;

    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
     * Called when the activity is starting. This is where most initialisation
     * should go: calling setContentView(int) to inflate the activity's UI, etc.
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of the
     * activity lifecycle executing. Derived classes must call through to the
     * super class's implementation of this method. If they do not, an exception
     * will be thrown.
     * 
     * @param icicle If the activity is being re-initialised after previously
     *            being shut down then this Bundle contains the data it most
     *            recently supplied in onSaveInstanceState(Bundle). Note:
     *            Otherwise it is null.
     */

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.sc_search);


        rdoType = (RadioGroup) findViewById(R.id.rdo_search_type);
        rdoUser = (RadioButton) findViewById(R.id.rdo_users);
        rdoTrack = (RadioButton) findViewById(R.id.rdo_tracks);

        txtQuery = (EditText) findViewById(R.id.query);

        btnSearch = (Button) findViewById(R.id.search);
        btnSearch.setEnabled(false);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSearch();
            }
        });

        rdoTrack.setVisibility(View.GONE);
        rdoUser.setVisibility(View.GONE);

        // account for special handling of this list if we are in a tab with
        // regards
        // to checking for data type (user/track) when restoring list data
            mList = CloudUtils.createList(this);

        ((FrameLayout) findViewById(R.id.list_holder)).addView(mList);
        mList.setVisibility(View.GONE);
        // mList.setFocusable(false);

        LazyBaseAdapter adpTrack = new TracklistAdapter(this, new ArrayList<Parcelable>());
        mTrackAdpWrapper = new LazyEndlessAdapter(this, adpTrack, "", CloudUtils.Model.track);

        LazyBaseAdapter adpUser = new UserlistAdapter(this, new ArrayList<Parcelable>());
        mUserAdpWrapper = new LazyEndlessAdapter(this, adpUser, "", CloudUtils.Model.user);

        mList.setAdapter(mTrackAdpWrapper);
        mList.setId(android.R.id.list);

        btnSearch.setNextFocusDownId(android.R.id.list);

        findViewById(R.id.search_root).setOnFocusChangeListener(keyboardHideFocusListener);
        findViewById(R.id.search_root).setOnClickListener(keyboardHideClickListener);
        findViewById(R.id.search_root).setOnTouchListener(keyboardHideTouchListener);

        txtQuery.setOnFocusChangeListener(queryFocusListener);
        txtQuery.setOnClickListener(queryClickListener);

        txtQuery.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSearch.setEnabled(s != null && s.length() > MIN_LENGTH);
            }
        });

        txtQuery.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && txtQuery.getText().length() > MIN_LENGTH) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public void onRefresh(boolean b) {
    }

    private void doSearch() {

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null)
            mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);

        rdoTrack.setVisibility(View.GONE);
        rdoUser.setVisibility(View.GONE);

        mTrackAdpWrapper.clear();
        mUserAdpWrapper.clear();

        mList.setVisibility(View.VISIBLE);

        if (rdoType.getCheckedRadioButtonId() == R.id.rdo_tracks) {
            mTrackAdpWrapper.setPath(CloudAPI.Enddpoints.PATH_TRACKS, URLEncoder.encode(txtQuery
                    .getText().toString()));
            mTrackAdpWrapper.createListEmptyView(mList);
            mList.setAdapter(mTrackAdpWrapper);

        } else {
            mUserAdpWrapper.setPath(CloudAPI.Enddpoints.USERS, URLEncoder.encode(txtQuery
                    .getText().toString()));
            mUserAdpWrapper.createListEmptyView(mList);
            mList.setAdapter(mUserAdpWrapper);

        }
    }

    public void setAdapterType(Boolean isUser) {
        if (isUser) {
            mList.setAdapter(mUserAdpWrapper);
            mUserAdpWrapper.createListEmptyView(mList);
        } else {
            mList.setAdapter(mTrackAdpWrapper);
            mTrackAdpWrapper.createListEmptyView(mList);
        }
    }

    private View.OnFocusChangeListener queryFocusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                rdoTrack.setVisibility(View.VISIBLE);
                rdoUser.setVisibility(View.VISIBLE);
            } else {
                // rdoTrack.setVisibility(View.GONE);
                // rdoUser.setVisibility(View.GONE);
            }
        }
    };

    private View.OnClickListener queryClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            rdoTrack.setVisibility(View.VISIBLE);
            rdoUser.setVisibility(View.VISIBLE);
        }
    };

    private View.OnFocusChangeListener keyboardHideFocusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            InputMethodManager mgr = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (hasFocus == true && mgr != null)
                mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            rdoTrack.setVisibility(View.GONE);
            rdoUser.setVisibility(View.GONE);
        }
    };

    private View.OnClickListener keyboardHideClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            InputMethodManager mgr = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mgr != null)
                mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            rdoTrack.setVisibility(View.GONE);
            rdoUser.setVisibility(View.GONE);
        }
    };

    private View.OnTouchListener keyboardHideTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            InputMethodManager mgr = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mgr != null)
                mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            rdoTrack.setVisibility(View.GONE);
            rdoUser.setVisibility(View.GONE);
            return false;
        }
    };
}
