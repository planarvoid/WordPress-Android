package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.api.Endpoints;

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

import java.net.URLEncoder;
import java.util.ArrayList;

// XXX decouple from ScActivity
public class ScSearch extends ScActivity {
    private Button btnSearch;
    private EditText txtQuery;

    private RadioGroup rdoType;
    private RadioButton rdoUser;
    private RadioButton rdoTrack;

    private LazyListView mList;
    private LazyEndlessAdapter mTrackAdpWrapper;
    private LazyEndlessAdapter mUserAdpWrapper;

    private static final int MIN_LENGTH = 2;

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
                doSearch(txtQuery.getText().toString());
            }
        });

        rdoTrack.setVisibility(View.GONE);
        rdoUser.setVisibility(View.GONE);

        // account for special handling of this list if we are in a tab with regards
        // to checking for data type (user/track) when restoring list data
        mList = buildList();

        ((FrameLayout) findViewById(R.id.list_holder)).addView(mList);
        mList.setVisibility(View.GONE);

        LazyBaseAdapter adpTrack = new TracklistAdapter(this, new ArrayList<Parcelable>(), Track.class);
        mTrackAdpWrapper = new LazyEndlessAdapter(this, adpTrack, "");

        LazyBaseAdapter adpUser = new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class);
        mUserAdpWrapper = new LazyEndlessAdapter(this, adpUser, "");

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
                    doSearch(txtQuery.getText().toString());
                    return true;
                }
                return false;
            }
        });
     }

    @Override
    protected void onResume() {
        super.onResume();
        pageTrack("/search");
    }

    void doSearch(final String query) {
        txtQuery.setText(query); // when called from Main

        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        rdoTrack.setVisibility(View.GONE);
        rdoUser.setVisibility(View.GONE);

        mTrackAdpWrapper.refresh(false);
        mUserAdpWrapper.refresh(false);

        mList.setVisibility(View.VISIBLE);

        if (rdoType.getCheckedRadioButtonId() == R.id.rdo_tracks) {
            mTrackAdpWrapper.setPath(Endpoints.TRACKS, URLEncoder.encode(query));
            mTrackAdpWrapper.createListEmptyView(mList);
            mList.setAdapter(mTrackAdpWrapper);
            mList.enableLongClickListener();
        } else {
            mUserAdpWrapper.setPath(Endpoints.USERS, URLEncoder.encode(query));
            mUserAdpWrapper.createListEmptyView(mList);
            mList.setAdapter(mUserAdpWrapper);
            mList.disableLongClickListener();
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
            if (hasFocus && mgr != null)
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
