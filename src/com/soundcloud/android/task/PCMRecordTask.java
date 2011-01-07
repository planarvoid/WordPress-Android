package com.soundcloud.android.task;

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.view.ScCreate;
import com.soundcloud.utils.PowerGauge;
import com.soundcloud.utils.Recorder;

public class PCMRecordTask  {
	private Recorder mRecorder;
	private File mRecordFile;
	
	private Context mAppContext;
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private PendingIntent mPendingIntent;
	private int mNotifyId = R.layout.cloudscrolltabs;
	
	private RecordListener listener;
	private Thread recordThread;
	private Thread progressThread;
	
	public AudioTrack playbackTrack;
	public int minSize;
	private boolean interrupted=false;
	
	public PCMRecordTask(Context c){
		mRecorder = new Recorder();
		mAppContext = c.getApplicationContext();	
		mNotificationManager = (NotificationManager) mAppContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	Handler progressHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (listener != null)
				listener.onRecProgressUpdate((int) mRecordFile.length());
			setOngoingNotification((int) Math.floor(CloudUtils.getPCMTime(mRecordFile, ScCreate.REC_SAMPLE_RATE, ScCreate.REC_CHANNELS, ScCreate.REC_BITS_PER_SAMPLE)));
		}
	};
	
	public void startRecording(){
		mRecorder.activate();
		
		createNotification(true);
		setOngoingNotification(0);
		
		recordInBackground();
	}
		 
	public void recordInBackground(){
		recordThread = new Thread() {
		      public void run() {
		    	  try {
		  			mRecorder.startRecording(mRecordFile);
		  			while (!Thread.interrupted()){
		  				progressHandler.sendMessage(new Message());
		  				Thread.sleep(200);
		  			}
		  			} catch (Exception e){
		  				e.printStackTrace();
		  			}
		  			mRecorder.stopRecording();
		    	}
		    };
		    recordThread.setPriority(Thread.MAX_PRIORITY);
		    recordThread.start();
		}
		  
	  public void stopRecording(){
		    interrupted=true;
		    if(recordThread!=null && recordThread.isAlive())
		    	recordThread.interrupt();
		    
			mRecorder.deactivate();
			mNotificationManager.cancel(mNotifyId);
			
			//mNotificationManager.cancel(mNotifyId);
			//showStoppedNotification((int) Math.floor(CloudUtils.getPCMTime(mRecordFile, ScCreate.REC_SAMPLE_RATE, ScCreate.REC_CHANNELS, ScCreate.REC_BITS_PER_SAMPLE)));
			
			if (listener != null)
				listener.onRecComplete(true);
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
	

	
	private void createNotification(Boolean ongoing){
		
		mNotificationManager.cancel(mNotifyId);
		
		
	    CharSequence tickerText = "";
	    if (ongoing)
	    	mAppContext.getResources().getString(R.string.cloud_recorder_notification_ticker);
	    else
	    	mAppContext.getResources().getString(R.string.cloud_recorder_notification_stopped_ticker);
    	
	    mNotification = new Notification(R.drawable.statusbar, tickerText, System.currentTimeMillis());	
    	if (ongoing) mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
    	
		 Intent i = new Intent(mAppContext, Dashboard.class);
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
