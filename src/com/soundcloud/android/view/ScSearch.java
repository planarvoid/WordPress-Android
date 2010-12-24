package com.soundcloud.android.view;

import java.net.URLEncoder;
import java.util.ArrayList;

import android.content.Context;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.activity.LazyTabActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
public class ScSearch extends ScTabView {


	


	// Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "ScSearch";
   
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
   
    private LazyActivity mActivity;
    
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
     * Called when the activity is starting.  This is where most
     * initialisation should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of
     * the activity lifecycle executing.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     * 
     * @param   icicle          If the activity is being re-initialised
     *                          after previously being shut down then this
     *                          Bundle contains the data it most recently
     *                          supplied in onSaveInstanceState(Bundle).
     *                          Note: Otherwise it is null.
     */
   
    
    public ScSearch(LazyActivity activity) {
		super(activity);
		
		mActivity = activity;
		
		LayoutInflater inflater = (LayoutInflater) activity
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.sc_search, this);
		
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
		
		// account for special handling of this list if we are in a tab with regards
		// to checking for data type (user/track) when restoring list data
		if (mActivity instanceof LazyTabActivity)
			mList = ((LazyTabActivity) mActivity).buildList(true);
		else
			mList = CloudUtils.createList(mActivity);
		
		((FrameLayout) findViewById(R.id.list_holder)).addView(mList);
		mList.setVisibility(View.GONE);
		//mList.setFocusable(false);
		
		LazyBaseAdapter adpTrack = new TracklistAdapter(mActivity, new ArrayList<Parcelable>());
		mTrackAdpWrapper = new LazyEndlessAdapter(mActivity,adpTrack,"",CloudUtils.Model.track);
		
		LazyBaseAdapter adpUser = new UserlistAdapter(mActivity, new ArrayList<Parcelable>());
		mUserAdpWrapper = new LazyEndlessAdapter(mActivity,adpUser,"",CloudUtils.Model.user);
		
		mList.setAdapter(mTrackAdpWrapper);
		mList.setId(android.R.id.list);
		
		btnSearch.setNextFocusDownId(android.R.id.list);
		
		this.setOnFocusChangeListener(keyboardHideFocusListener);
		this.setOnClickListener(keyboardHideClickListener);
		this.setOnTouchListener(keyboardHideTouchListener);
		
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
    
    private void doSearch(){
    	
    	InputMethodManager mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (mgr != null) mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    	
		rdoTrack.setVisibility(View.GONE);
		rdoUser.setVisibility(View.GONE);

		mTrackAdpWrapper.clear();
		mUserAdpWrapper.clear();
		
		mList.setVisibility(View.VISIBLE);
		
    	if (rdoType.getCheckedRadioButtonId() == R.id.rdo_tracks){
    		mTrackAdpWrapper.setPath(CloudCommunicator.PATH_TRACKS,URLEncoder.encode(txtQuery.getText().toString()));
    		mTrackAdpWrapper.createListEmptyView(mList);
        	mList.setAdapter(mTrackAdpWrapper);
        		
    	} else {
    		mUserAdpWrapper.setPath(CloudCommunicator.PATH_USERS,URLEncoder.encode(txtQuery.getText().toString()));
    		mUserAdpWrapper.createListEmptyView(mList);
        	mList.setAdapter(mUserAdpWrapper);
        	
    	}
    }
    
    public void setAdapterType(Boolean isUser){
    	if (isUser)
    		mList.setAdapter(mUserAdpWrapper);
    	else
    		mList.setAdapter(mTrackAdpWrapper);
    }
    
    private OnFocusChangeListener queryFocusListener = new View.OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			Log.i(TAG,"Query listener " + hasFocus);
			if (hasFocus){
				rdoTrack.setVisibility(View.VISIBLE);
				rdoUser.setVisibility(View.VISIBLE);
			} else {
				//rdoTrack.setVisibility(View.GONE);
				//rdoUser.setVisibility(View.GONE);
			}
		}
	};
	
	private OnClickListener queryClickListener = new View.OnClickListener() {
		 public void onClick(View v) {
			 rdoTrack.setVisibility(View.VISIBLE);
				rdoUser.setVisibility(View.VISIBLE);
		 }
	};
    
    private OnFocusChangeListener keyboardHideFocusListener = new View.OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			InputMethodManager mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (hasFocus == true && mgr != null) mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			rdoTrack.setVisibility(View.GONE);
			rdoUser.setVisibility(View.GONE);
		}
	};
	
	 private OnClickListener keyboardHideClickListener = new View.OnClickListener() {
		 public void onClick(View v) {
				InputMethodManager mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (mgr != null) mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				rdoTrack.setVisibility(View.GONE);
				rdoUser.setVisibility(View.GONE);
		 }
	};
	
	 private OnTouchListener keyboardHideTouchListener = new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			InputMethodManager mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (mgr != null) mgr.hideSoftInputFromWindow(txtQuery.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			rdoTrack.setVisibility(View.GONE);
			rdoUser.setVisibility(View.GONE);
			return false;
		}
	 };
}
