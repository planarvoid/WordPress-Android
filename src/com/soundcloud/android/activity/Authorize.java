package com.soundcloud.android.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.android.R;

public class Authorize extends Activity {
	
	private static final String TAG = "Authorize";
	
	private SharedPreferences mPreferences;
	private CloudCommunicator mCloudComm;
	
	private EditText txtUsername;
	private EditText txtPassword;
	private Button btnAuthorize;
	
	private RelativeLayout authBg;
	
	private static final int DIALOG_AUTHENTICATING = 10;
	private static final int DIALOG_AUTHENTICATION_FAILED = 11;
	
	private Handler mHandler = new Handler();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authorize);
		
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mCloudComm = CloudCommunicator.getInstance(this);
		
		authBg = (RelativeLayout) findViewById(R.id.auth_bg);
		
		authBg.setOnFocusChangeListener(keyboardHideFocusListener);
		authBg.setOnClickListener(keyboardHideClickListener);
		authBg.setOnTouchListener(keyboardHideTouchListener);
		
		txtUsername = (EditText) findViewById(R.id.txt_username);
		txtPassword = (EditText) findViewById(R.id.txt_password);
		
		btnAuthorize = (Button) findViewById(R.id.btn_authorize);
		btnAuthorize.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onAuthorize();
			}
		});
	}
	
	private void onAuthorize(){
		Thread thread = new Thread(new Runnable()
        {
                public void run()
                {
                        try
                        {
                          mCloudComm.getApi().obtainAccessToken(txtUsername.getText().toString(), txtPassword.getText().toString());
                        } catch (Exception e) {
                        	e.printStackTrace();
                        	
                        } 
                        
                        mHandler.post(new Runnable() {
                            public void run() {
                         	   onAuthReturn();
                            }
                        });
                }
        });
		
		showDialog(this.DIALOG_AUTHENTICATING);
		thread.start();
	}
	
	private void onAuthReturn(){
		Log.i("AUTH","Auth returned " + mCloudComm.getState());
		removeDialog(DIALOG_AUTHENTICATING);
	}
	
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			
			case DIALOG_AUTHENTICATING:
				ProgressDialog progressDialog = new ProgressDialog(this);
				progressDialog.setTitle(R.string.authenticating_title);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setIndeterminate(true);
				progressDialog.setCancelable(false);
			
			return progressDialog;
		}
		return null;
	}
	
	
	
	
		
	 private OnFocusChangeListener keyboardHideFocusListener = new View.OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				InputMethodManager mgr = (InputMethodManager) getSystemService(Authorize.this.INPUT_METHOD_SERVICE);
				if (hasFocus == true && mgr != null) mgr.hideSoftInputFromWindow(txtUsername.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
		};
		
		 private OnClickListener keyboardHideClickListener = new View.OnClickListener() {
			 public void onClick(View v) {
					InputMethodManager mgr = (InputMethodManager) getSystemService(Authorize.this.INPUT_METHOD_SERVICE);
					if (mgr != null) mgr.hideSoftInputFromWindow(txtUsername.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			 }
		};
		
		 private OnTouchListener keyboardHideTouchListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				InputMethodManager mgr = (InputMethodManager) getSystemService(Authorize.this.INPUT_METHOD_SERVICE);
				if (mgr != null) mgr.hideSoftInputFromWindow(txtUsername.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				return false;
			}
		 };
	
}
