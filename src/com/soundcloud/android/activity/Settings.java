package com.soundcloud.android.activity;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Menu;

import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.android.R;

public class Settings extends PreferenceActivity {
	private static final int MENU_CACHE = Menu.FIRST;
	private static final int MENU_USER_CLEAR = 2;
	private static final int MENU_USER_CONNECT = 3;

	private static final int DIALOG_CACHE_DELETED = 10;
	private static final int DIALOG_CACHE_DELETING = 11;
	
	private static final int DIALOG_USER_DELETE_CONFIRM = 12;
	private static final int DIALOG_USER_DELETED = 13;

	private ProgressDialog mDeleteDialog;
	private DeleteCacheTask mDeleteTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.settings);
		this.findPreference("revokeAccess").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_USER_DELETE_CONFIRM);
                return true;
        }
		});
		this.findPreference("clearCache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				mDeleteTask = new DeleteCacheTask();
				mDeleteTask.activity = Settings.this;
				mDeleteTask.execute();
                return true;
        }
		});
		
	}
	
	

	
	
	private void connectUser(){
		
		try {
			CloudCommunicator.getInstance(this).launchAuthorization();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_CACHE_DELETED:
				return new AlertDialog.Builder(this).setTitle(R.string.cache_cleared).setPositiveButton(android.R.string.ok, null).create();
			case DIALOG_CACHE_DELETING:
				if (mDeleteDialog == null) {
					mDeleteDialog = new ProgressDialog(this);
					mDeleteDialog.setTitle(R.string.cache_clearing);
					mDeleteDialog.setMessage(getString(R.string.cache_clearing_message));
					mDeleteDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					mDeleteDialog.setIndeterminate(true);
					mDeleteDialog.setCancelable(false);
				}
				return mDeleteDialog;
				
			 case DIALOG_USER_DELETE_CONFIRM:
		            return new AlertDialog.Builder(this)
		                .setTitle(R.string.menu_clear_user_title)
		                .setMessage(R.string.menu_clear_user_desc)
		                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    	clearUserData();
		                    }
		                })
		                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		                    public void onClick(DialogInterface dialog, int whichButton) {
		                    	
		                    }
		                })
		                .create();
		}
		return super.onCreateDialog(id);
	}
	
	private void clearUserData() {
		CloudCommunicator.getInstance(this).clearSoundCloudAccount();
	}
	
	



	private static class DeleteCacheTask extends AsyncTask<String, Integer, Boolean> {
		public Settings activity;

		@Override
		protected void onPreExecute() {
			activity.showDialog(DIALOG_CACHE_DELETING);
		}

		@Override
		protected Boolean doInBackground(String... params) {
			File folder = new File(activity.getCacheDir().toString());
			File[] files = folder.listFiles();
			File file;
			int length = files.length;
			for (int i = 0; i < length; i++) {
				file = files[i];
				if (file.isFile()) {
					file.delete();
					publishProgress(i, length);
				}
			}
			return true;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			activity.mDeleteDialog.setIndeterminate(false);
			activity.mDeleteDialog.setMax(progress[1]);
			activity.mDeleteDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			activity.removeDialog(DIALOG_CACHE_DELETING);
			activity.showDialog(DIALOG_CACHE_DELETED);
		}
	}

}
