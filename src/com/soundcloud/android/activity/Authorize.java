package com.soundcloud.android.activity;

import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

public class Authorize extends ScActivity {
	
	private static final String TAG = "Authorize";
	
	private SharedPreferences mPreferences;
	
	private EditText txtUsername;
	private EditText txtPassword;
	private Button btnAuthorize;
	
	private RelativeLayout authBg;
	
	private static final int DIALOG_AUTHENTICATING = 10;
	private static final int DIALOG_AUTHENTICATION_FAILED = 11;
	
	private Handler mHandler = new Handler();
	
	private int[] mExchangePostDelays = {1000,3000,10000};
	private int mCurrentExchangeRetries = 0;

	private String oAuthToken;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.authorize);
		setContentView(R.layout.main);
		
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		/*authBg = (RelativeLayout) findViewById(R.id.auth_bg);
		
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
		*/
		// extract the OAUTH access token if it exists
		Uri uri = this.getIntent().getData();
		if(uri != null) {
			Log.i(TAG,"AUTHORIZATION TOKEN " + oAuthToken);
		  	oAuthToken = uri.getQueryParameter("oauth_verifier");	
		  	mHandler.post(mExchangeToken);
		}
	}
	
	private void onAuthorize(){
		Thread thread = new Thread(new Runnable()
        {
                public void run()
                {
                        try
                        {
                          //mCloudComm.obtainAccessToken(txtUsername.getText().toString(), txtPassword.getText().toString());
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
		Log.i("AUTH","Auth returned " + ((SoundCloudApplication) getApplication()).getState());
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
	
	

	private void onAuthenticated(){
		Intent intent = new Intent(this, Dashboard.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		//finish();
	}
	
	
	
	private Runnable mExchangeToken = new Runnable() {
		   public void run() {
			   
			   if (mCurrentExchangeRetries == 1)
				   showDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_RETRY);
			   
		        if (oAuthToken != "") {
		        	Log.i(TAG,"Updating TOKEN " + oAuthToken);
		        	getSoundCloudApplication().updateAuthorizationStatus(oAuthToken);
		            if (getSoundCloudApplication().getState() != SoundCloudAPI.State.AUTHORIZED) {
		                if (mCurrentExchangeRetries < mExchangePostDelays.length) {
		                    mHandler.postDelayed(mExchangeToken, mExchangePostDelays[mCurrentExchangeRetries]);
		                    mCurrentExchangeRetries++;		                    
		                } else {
		                	Log.i(TAG," TOKEN update successful ");
		                	getSoundCloudApplication().clearTokens();
		                	removeDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_RETRY);
		                    showDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_ERROR);
		                }
		            } else {
		            	onAuthenticated();
		            }
		        }
		   }
		};

	
		
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
