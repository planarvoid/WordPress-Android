package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.ScListView;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;

// XXX decouple from ScActivity
public class ScSearch extends ScActivity {
    private EditText txtQuery;

    private RadioGroup rdoType;
    private RadioButton rdoUser;
    private RadioButton rdoTrack;

    private ScListView mList;
    private LazyEndlessAdapter mTrackAdpWrapper;
    private LazyEndlessAdapter mUserAdpWrapper;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.sc_search);

        rdoType = (RadioGroup) findViewById(R.id.rdo_search_type);
        rdoUser = (RadioButton) findViewById(R.id.rdo_users);
        rdoTrack = (RadioButton) findViewById(R.id.rdo_tracks);

        txtQuery = (EditText) findViewById(R.id.query);

        Button btnSearch = (Button) findViewById(R.id.search);
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

        ((ViewGroup) findViewById(R.id.list_holder)).addView(mList);
        mList.setVisibility(View.GONE);

        mTrackAdpWrapper = new LazyEndlessAdapter(this, new TracklistAdapter(this, new ArrayList<Parcelable>(), Track.class), null);
        mUserAdpWrapper = new LazyEndlessAdapter(this, new UserlistAdapter(this, new ArrayList<Parcelable>(), User.class), null);

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
        } else {
            setListType(false);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[]{
                mList.getBaseAdapter().getLoadModel(),
                mList.getVisibility(),
                mTrackAdpWrapper.saveState(),
                mUserAdpWrapper.saveState()
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
        pageTrack("/search");
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
            mTrackAdpWrapper.setRequest(Request.to(Endpoints.TRACKS).with("q", query));
            setListType(false);
            mList.enableLongClickListener();
            mUserAdpWrapper.reset(false,false);
        } else {
            mUserAdpWrapper.setRequest(Request.to(Endpoints.USERS).with("q", query));
            setListType(true);
            mList.disableLongClickListener();
            mTrackAdpWrapper.reset(false,false);
        }

        mList.setLastUpdated(0);
        mList.onRefresh();
        mList.setVisibility(View.VISIBLE);
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
