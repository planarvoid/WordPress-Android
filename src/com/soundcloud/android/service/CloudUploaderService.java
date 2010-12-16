package com.soundcloud.android.service;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.RemoteViews;

import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.task.VorbisEncoderTask;
import com.soundcloud.android.task.WavEncoderTask;


public class CloudUploaderService extends Service {

	//public static final String ServletUri = "http://" + AppUtils.EmulatorLocalhost + ":8080/Ping";
	private static final String TAG = "CloudUploaderService";

	private static ArrayBlockingQueue<Track> _downloadlist = new ArrayBlockingQueue<Track>(20);
	
	
	private static final int UPLOAD_NOTIFY_ID = R.layout.sc_create;
	
	private static PowerManager mPowerManager;
	private static WakeLock mWakeLock;
	
	//private WavEncoderTask mWavTask;
	private VorbisEncoderTask mOggTask;
	private UploadTask mUploadTask;
	
	
	private RemoteViews notificationView;
	private Notification mNotification;
	protected CloudCommunicator mCloudComm;
	
	private HashMap<String,String> mUploadingData;
	private int mCurrentDownloadPercentage;
	private int _lastDownloadPercentage;
	private NotificationManager nm;
	
	private DBAdapter db;
	
	private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mResumeAfterCall = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mQuietMode = false;
    
    private String mOggFilePath;
	
	
	public static void setMainActivity(LazyActivity mainActivity) {
		
	}
	
	protected void acquireWakeLock() {
		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
	}

	protected void releaseWakeLock() {
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}


	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// lifecycle methods
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	 @Override
	    public IBinder onBind(Intent intent) {
	        mServiceInUse = true;
	        return mBinder;
	    }

	    @Override
	    public void onRebind(Intent intent) {
	        mServiceInUse = true;
	    }

	    @Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
	        mServiceStartId = startId;
	        return START_STICKY;
	    }
	    
	    @Override
	    public boolean onUnbind(Intent intent) {
	        mServiceInUse = false;
	        Log.i(TAG,"ON UNBIND " + isUploading());

	        // No active playlist, OK to stop the service right now
	        stopSelf(mServiceStartId);
	        return true;
	    }
	    
	  
	@Override
	public void onCreate() {
	  super.onCreate();

	  // init the service here
	  _startService();
	  
	  // get notification manager
	  nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
	  mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	  mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
	 

	}
	
	private void _startService() {
	  Log.i(getClass().getSimpleName(), "Download Service Started started!!!");
	}
	
	

	@Override
	public void onDestroy() {
	  super.onDestroy();
	
	  if (isUploading()) {
          Log.e(getClass().getSimpleName(), "Service being destroyed while still playing.");
      }
	  
	  _shutdownService();
	}

	private void gotoIdleState(){
		stopForeground(true);
	}
	

	private void _shutdownService() {
	  Log.i(getClass().getSimpleName(), "Download Service stopped!!!");
	  
	  freeWakeLock();
	 
	  if (mOggTask != null)
		  mOggTask.cancel(true);
	  
	  if (mUploadTask != null)
		  mUploadTask.cancel(true);
	  
	  Log.i(getClass().getSimpleName(), "Download Service shutdown complete.");
	  
	}
	

	
	@SuppressWarnings("unchecked")
	private void startUpload(Map trackdata) {

		Iterator it = trackdata.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        System.out.println(pairs.getKey() + " = " + pairs.getValue());
	    }
		
		mUploadingData = (HashMap<String, String>) trackdata;
		

		 it = mUploadingData.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        System.out.println("-------" + pairs.getKey() + " = " + pairs.getValue());
	    }
	    
    	int icon = R.drawable.statusbar;
	    CharSequence tickerText = getString(R.string.cloud_uploader_notification_ticker);
	    long when = System.currentTimeMillis();
    	mNotification = new Notification(icon, tickerText, when);	
    	
    	 Intent i = new Intent(this, Main.class);
         i.addCategory(Intent.CATEGORY_LAUNCHER);
         i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
         i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         i.setAction(Intent.ACTION_MAIN);
        
         notificationView = new RemoteViews(getPackageName(), R.layout.status_upload);
         
         //CharSequence titleText = getString(R.string.cloud_downloader_event_title_track);
		 CharSequence trackText =  mUploadingData.get("track[title]").toString();
         notificationView.setTextViewText(R.id.message, trackText);
         notificationView.setTextViewText(R.id.percentage, "0");
         notificationView.setProgressBar(R.id.progress_bar, 100, 0, true);
         
         mNotification.contentView = notificationView;
         mNotification.contentIntent = PendingIntent.getActivity(this, 0, i, 0);
	   	 mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

    	
    	startForeground(UPLOAD_NOTIFY_ID,mNotification);
		
    	
    	mOggFilePath = getApplicationContext().getCacheDir() + "/tmp.ogg";
    	mOggTask = new EncodeOggTask();
		mOggTask.execute(mUploadingData.get("pcm_path"), mOggFilePath);
    
    	
		
	}
	
	
    
	    
		private void freeWakeLock() {
			if (mWakeLock.isHeld()) {
				Log.d(TAG, "Freeing wakelock");
				mWakeLock.release();
			}
		}
	    
		
		public Boolean isUploading(){
			return (mOggTask != null || mUploadTask != null);
		}
		
		

	   
	    
	     public void onPCMEncodeComplete(Boolean result){
	    	
	    	if (result){
	    	
	    		
	    	} else {
	    		
	    	}
	    }
		
	   
	    public void onOggEncodeComplete(Boolean result){
	    	if (result){
	    		mOggTask = null;
	    		
	    		Log.i(TAG,"Start Uploading");
	    		
	    		final List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();
	    		
	    		Iterator it = mUploadingData.entrySet().iterator();
	    	    while (it.hasNext()) {
	    	        Map.Entry pairs = (Map.Entry)it.next();
	    	        Log.i("---------", pairs.getKey() + " = " + pairs.getValue());
	    	        
	    	        if (!(pairs.getKey().toString().contentEquals("pcm_path") || pairs.getKey().toString().contentEquals("image_path")))
	    	        		params.add(new BasicNameValuePair(pairs.getKey().toString(),pairs.getValue().toString()));
	    	       
	    	    }
	    		
	    	    mUploadTask = new UploadOggTask();
	    	    mUploadTask.trackFile = new File(mOggFilePath);
	    	    mUploadTask.trackParams = params;
	    	    if (!CloudUtils.stringNullEmptyCheck(mUploadingData.get("artwork_path"))) mUploadTask.artworkFile = new File(mUploadingData.get("artwork_path"));
	    	    mUploadTask.execute();
	    		
	    	} else {
	    		
	    	}
	    }
	    
	    public void onOggUploadComplete(Boolean result){
	    	if (result){
	    		mUploadTask = null;
	    		nm.cancel(UPLOAD_NOTIFY_ID);
	    		gotoIdleState();
	    	} else {
	    		
	    	}
	    }
		
		 private class EncodePCMTask extends WavEncoderTask {

			 private String eventString;
			 
				@Override
				protected void onPreExecute() {
					eventString = getApplicationContext().getString(R.string.cloud_uploader_event_preparing);
			        notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
				}


				@Override
				protected void onProgressUpdate(Integer... progress) {
					notificationView.setProgressBar(R.id.progress_bar, progress[1], progress[0], false);
					notificationView.setTextViewText(R.id.percentage, String.format(eventString, (100*progress[0])/progress[1]));
					 nm.notify(UPLOAD_NOTIFY_ID, mNotification);
				}

				@Override
				protected void onPostExecute(Boolean result) {
					onPCMEncodeComplete(result);
				}
			}
		
		
		 private class EncodeOggTask extends VorbisEncoderTask {

			 private String eventString;
			 
			 @Override
				protected void onPreExecute() {
					eventString = getApplicationContext().getString(R.string.cloud_uploader_event_encoding);
			        notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
				}


				@Override
				protected void onProgressUpdate(Integer... progress) {
					notificationView.setProgressBar(R.id.progress_bar, progress[1], progress[0], false);
					notificationView.setTextViewText(R.id.percentage, String.format(eventString, (100*progress[0])/progress[1]));
					 nm.notify(UPLOAD_NOTIFY_ID, mNotification);
				}

				@Override
				protected void onPostExecute(Boolean result) {
					onOggEncodeComplete(result);
				}
			}
		 
		 private class UploadOggTask extends UploadTask {
			 
			 private String eventString;
			 
				@Override
				protected void onPreExecute() {
					eventString = getApplicationContext().getString(R.string.cloud_uploader_event_uploading);
			        notificationView.setTextViewText(R.id.percentage, String.format(eventString, 0));
				}


				@Override
				protected void onProgressUpdate(Integer... progress) {
					notificationView.setProgressBar(R.id.progress_bar, progress[1], progress[0], false);
					notificationView.setTextViewText(R.id.percentage, String.format(eventString, (100*progress[0])/progress[1]));
					 nm.notify(UPLOAD_NOTIFY_ID, mNotification);
					
				}

				@Override
				protected void onPostExecute(Boolean result) {
					onOggUploadComplete(result);
				}
			}
		


	
	    /*
	     * By making this a static class with a WeakReference to the Service, we
	     * ensure that the Service can be GCd even when the system process still
	     * has a remote reference to the stub.
	     */
	    static class ServiceStub extends ICloudUploaderService.Stub {
	        WeakReference<CloudUploaderService> mService;
	        
	       
			public ServiceStub(CloudUploaderService cloudUploaderService) {
				 mService = new WeakReference<CloudUploaderService>(cloudUploaderService);
			}


			@Override
			public void uploadTrack(Map trackdata) throws RemoteException {
				mService.get().startUpload(trackdata);
			}
	    }
	    
	    private final IBinder mBinder = new ServiceStub(this);

}//end class MyService
