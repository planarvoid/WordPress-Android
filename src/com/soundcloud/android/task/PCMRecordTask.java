package com.soundcloud.android.task;

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.view.ScCreate;
import com.soundcloud.utils.PowerGauge;
import com.soundcloud.utils.Recorder;

public class PCMRecordTask extends AsyncTask<String, Integer, Boolean> {
	private Recorder mRecorder;
	private File mRecordFile;
	private Boolean mRecording;
	
	private Context mAppContext;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private PendingIntent mPendingIntent;
	private int mNotifyId = R.layout.cloudscrolltabs;
	
	private RecordListener listener;
	
	public AudioTrack playbackTrack;
	public int minSize;
	
	
	
	public interface stages {
		int reading = 1;
		int writing = 2;
	}
	
	public PCMRecordTask(Context c){
		mRecorder = new Recorder();
		mAppContext = c.getApplicationContext();	
		mNotificationManager = (NotificationManager) mAppContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	
	
	public void setRecordListener(RecordListener recordListener){
		listener = recordListener;
	}
	
	public void setRecordFile(File recordFile){
		mRecordFile = recordFile;
	}
	
	public void setPowerGauge(PowerGauge powerGauge){
		mRecorder.setPowerGauge(powerGauge);
		
	}
	
	public void stopRecording(){
		mRecording = false;
	}

	@Override
	protected void onPreExecute() {
		mRecording = true;
		mRecorder.activate();
		
		createNotification(true);
		setOngoingNotification(0);
		
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		if (listener != null)
			listener.onRecProgressUpdate((int) mRecordFile.length());
		
		setOngoingNotification((int) Math.floor(CloudUtils.getPCMTime(mRecordFile, ScCreate.REC_SAMPLE_RATE, ScCreate.REC_CHANNELS, ScCreate.REC_BITS_PER_SAMPLE)));
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mRecorder.deactivate();
		mNotificationManager.cancel(mNotifyId);
		
		//mNotificationManager.cancel(mNotifyId);
		//showStoppedNotification((int) Math.floor(CloudUtils.getPCMTime(mRecordFile, ScCreate.REC_SAMPLE_RATE, ScCreate.REC_CHANNELS, ScCreate.REC_BITS_PER_SAMPLE)));
		
		if (listener != null)
			listener.onRecComplete(result);
	}

	@Override
	protected Boolean doInBackground(String... params) {
		try {
			mRecorder.startRecording(mRecordFile);
			while (mRecording){
				publishProgress();
				try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
			}
			mRecorder.stopRecording();
    	 return true;
		} catch (Exception e){
			return false;
		}
	}
	
	private void createNotification(Boolean ongoing){
		
		mNotificationManager.cancel(mNotifyId);
		
		
	    CharSequence tickerText = "";
	    if (ongoing)
	    	mAppContext.getResources().getString(R.string.cloud_recorder_notification_ticker);
	    else
	    	mAppContext.getResources().getString(R.string.cloud_recorder_notification_stopped_ticker);
    	
	    mNotification = new Notification(R.drawable.statusbar, tickerText, System.currentTimeMillis());	
    	if (ongoing) mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
    	
		 Intent i = new Intent(mAppContext, Main.class);
		 i.addCategory(Intent.CATEGORY_LAUNCHER);
		 i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
		 i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		 i.setAction(Intent.ACTION_MAIN);
		 
		 mPendingIntent = PendingIntent.getActivity(mAppContext, 0, i, 0);
	}
	
	private void setOngoingNotification(int i){
		 mNotification.setLatestEventInfo(mAppContext, 
				 mAppContext.getString(R.string.cloud_recorder_event_title), 
	 				String.format(mAppContext.getString(R.string.cloud_recorder_event_message),i),
	 				mPendingIntent);
		 mNotificationManager.notify(mNotifyId,mNotification);
	}
	
	private void showStoppedNotification(int i){
		createNotification(false);
		
		 mNotification.setLatestEventInfo(mAppContext, 
				 mAppContext.getString(R.string.cloud_recorder_stopped_event_title), 
	 				String.format(mAppContext.getString(R.string.cloud_recorder_stopped_event_message),i),
	 				mPendingIntent);
		 
		 mNotificationManager.notify(mNotifyId,mNotification);
	}


	// Define our custom Listener interface
	public interface RecordListener {
		public abstract void onRecProgressUpdate(int position);
		public abstract void onRecComplete(boolean result);
	}
	
}
