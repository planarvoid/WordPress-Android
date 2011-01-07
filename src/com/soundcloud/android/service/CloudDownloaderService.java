package com.soundcloud.android.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.RemoteViews;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;


public class CloudDownloaderService extends Service {

	//public static final String ServletUri = "http://" + AppUtils.EmulatorLocalhost + ":8080/Ping";
	private static final String TAG = "CloudDownloaderService";

	private static ArrayBlockingQueue<Track> _downloadlist = new ArrayBlockingQueue<Track>(20);
	
	public static final int DOWNLOADSERVICE_STATUS = 1;
	public static final String DOWNLOAD_STARTED = "com.overcast.overcast.downloadservicecommand.downloadstarted";
    public static final String DOWNLOAD_FINISHED = "com.overcast.overcast.downloadservicecommand.downloadfinished";
    public static final String DOWNLOAD_ERROR = "com.overcast.overcast.downloadservicecommand.downloaderror";
	private static final int DOWNLOAD_NOTIFY_ID = R.layout.refresh_bar;
	
	private static PowerManager mPowerManager;
	private static WakeLock mWakeLock;
	
	private DownloadTask mDownloadTask;
	private RemoteViews notificationView;
	private Notification mNotification;
	
	private Track mDownloadingData;
	private int mCurrentDownloadPercentage;
	private int _lastDownloadPercentage;
	private NotificationManager nm;
	
	private DBAdapter db;
	
	private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private boolean mResumeAfterCall = false;
    private boolean mIsSupposedToBePlaying = false;
    private boolean mQuietMode = false;
	
	
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
	        
	        Log.i(TAG,"ON UNBIND " + isDownloading());

	        if (isDownloading() || mResumeAfterCall) {
	            // something is currently playing, or will be playing once 
	            // an in-progress call ends, so don't stop the service now.
	            return true;
	        }
	        
	        Log.i(TAG,"stopping self");
	        
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
	
	  if (isDownloading()) {
          Log.e(getClass().getSimpleName(), "Service being destroyed while still playing.");
      }
	  
	  _shutdownService();
	}


	

	private void _shutdownService() {
	  //if (timer != null) timer.cancel();
	  Log.i(getClass().getSimpleName(), "Download Service stopped!!!");
	  
	  freeWakeLock();
	 
	  if (mDownloadTask != null)
		  mDownloadTask.cancel(true);
	  
	  Log.i(getClass().getSimpleName(), "Download Service shutdown complete.");
	  
	}
	
	private void addTrackToDownloadQueue(Track trackdata){
		Log.i(getClass().getSimpleName(), "add track to playlist " + trackdata.getData(Track.key_title));
		
		Boolean _found = false;
		
		if (mDownloadingData != null){
			if (mDownloadingData.getData(Track.key_id).contentEquals(trackdata.getData(Track.key_id)))
				_found = true;
		}
		
		if (!_found)
		for (Track pendingTrack : _downloadlist){
			if (pendingTrack.getData(Track.key_id).contentEquals(trackdata.getData(Track.key_id))){
				_found = true;
			}
		}
		
		if (!_found)
			_downloadlist.add(trackdata);
		
		Log.i(getClass().getSimpleName(), "track added, is downloading? " + isDownloading());
		
		if (!isDownloading())
			nextTrackInDownloadList();
	}
	
	private Boolean nextTrackInDownloadList(){
		Log.i(TAG, "nextTrackInDownloadList empty? " + _downloadlist.isEmpty());
		if (!_downloadlist.isEmpty()){
			loadDownload(_downloadlist.remove());
			return true;
		} else {
			gotoIdleState();
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private void loadDownload(Track track) {
		Log.i(TAG, "Load Download " + track.getData(Track.key_title));
		mDownloadingData = track;
		String userDirectory = track.getData(Track.key_user_permalink);
		Log.d(TAG, "Checking download directory : " + CloudUtils.MUSIC_DIRECTORY + "/"+ userDirectory);
		
		File dirCheck = new File(CloudUtils.MUSIC_DIRECTORY + "/"+ userDirectory);
		if (!dirCheck.exists())
			dirCheck.mkdirs();

		dirCheck = new File(CloudUtils.ARTWORK_DIRECTORY+ "/"+ userDirectory);
		if (!dirCheck.exists())
			dirCheck.mkdirs();

		dirCheck = new File(CloudUtils.WAVEFORM_DIRECTORY+ "/"+ userDirectory);
		if (!dirCheck.exists())
			dirCheck.mkdirs();
		
		dirCheck = new File(CloudUtils.AVATAR_DIRECTORY);
		if (!dirCheck.exists())
			dirCheck.mkdirs();
		

    	int icon = R.drawable.statusbar;
	    CharSequence tickerText = getString(R.string.cloud_downloader_notification_ticker);
	    long when = System.currentTimeMillis();
    	mNotification = new Notification(icon, tickerText, when);	
    	
    	 Intent i = new Intent(this, Dashboard.class);
         i.addCategory(Intent.CATEGORY_LAUNCHER);
         i.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
         i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         i.setAction(Intent.ACTION_MAIN);
        
         notificationView = new RemoteViews(getPackageName(), R.layout.status_upload);
         
         //CharSequence titleText = getString(R.string.cloud_downloader_event_title_track);
		 CharSequence trackText = mDownloadingData.getData(User.key_username).toString() + " - " + mDownloadingData.getData(Track.key_title).toString();
		 CharSequence percentageText = String.format(getString(R.string.cloud_downloader_event_download_avatar), 0);
         
         //notificationView.setTextViewText(R.id.title, titleText);
         notificationView.setTextViewText(R.id.trackname, trackText);
         notificationView.setTextViewText(R.id.percentage, percentageText);
         notificationView.setProgressBar(R.id.progress_bar, 100, 0, false);
         
         mNotification.contentView = notificationView;
         mNotification.contentIntent = PendingIntent.getActivity(this, 0, i, 0);
	   	 mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

    	
    	startForeground(DOWNLOAD_NOTIFY_ID,mNotification);
		
    	mDownloadTask = new DownloadTask();
		mDownloadTask.execute(track.mapData());
		
	}
	
	
    private Boolean isDownloading(){
    	
    	if (mDownloadTask != null){
    		if (mDownloadTask.getStatus() != AsyncTask.Status.FINISHED)
    			return true;
    	} 
    	
    	return false;
    }
    
    /**
     * Returns the path of the currently downloading file, or null if
     * no file is currently playing.
     */
    public Track getDownloadingData() {
        return mDownloadingData;
    }
    
    /**
     * Returns the path of the currently downloading file, or null if
     * no file is currently playing.
     */
    public String getCurrentDownloadId() {
    	if (mDownloadingData != null)
    		return mDownloadingData.getData(Track.key_id);
    	else
    		return null;
    }
    
    /**
     * Returns the current downloading percentage as an integer or null
     * no file is currently downloading.
     */
    public int getCurrentDownloadPercentage() {
        return mCurrentDownloadPercentage;
    }
    
	    
		private void freeWakeLock() {
			if (mWakeLock.isHeld()) {
				Log.d(TAG, "Freeing wakelock");
				mWakeLock.release();
			}
		}
	    
		
		private void notifyChange(String what) {
			Log.d(TAG, "Notifying change " + what);
	        Intent i = new Intent(what);
	        i.putExtra("id", getCurrentDownloadId());
	        sendBroadcast(i);
//	        
//	        if (what.equals(QUEUE_CHANGED)) {
//	            saveQueue(true);
//	        } else {
//	            saveQueue(false);
//	        }
	        
	        // Share this notification directly with our widgets
	        //mAppWidgetProvider.notifyChange(this, what);
	    }
	    
	   
		private class DownloadTask extends AsyncTask<HashMap<String, String>, Integer, Boolean> {
			private boolean m_cancelled = false;
			
			@Override
			protected void onPreExecute() {
				//activity.startProgress();
				acquireWakeLock();
				mCurrentDownloadPercentage = 0;
				_lastDownloadPercentage = -1;
				notifyChange(DOWNLOAD_STARTED);
			
			}

			@Override
			protected void onProgressUpdate(Integer... progress) {
				Log.d(TAG, "progress : " + progress[1] + "|" + progress[2]);
				
				mCurrentDownloadPercentage = (int)((((float) progress[1])/((float)progress[2]))*100);
				mDownloadingData.putData(Track.key_kb_loaded, Integer.toString((int) progress[1]/1000));
				mDownloadingData.putData(Track.key_kb_total, Integer.toString((int) progress[2]/1000));
				mDownloadingData.putData("progress", Integer.toString(mCurrentDownloadPercentage));
				
				if (_lastDownloadPercentage != mCurrentDownloadPercentage){
					_lastDownloadPercentage = mCurrentDownloadPercentage;
					showDownloadNotificationProgress(progress[0],mCurrentDownloadPercentage);
				}
				
				//showDownloadNotificationProgress()
				acquireWakeLock();
			}

			@Override
			public void onCancelled() {
				m_cancelled = true;
				gotoIdleState();
			}
			

			@Override
			protected Boolean doInBackground(HashMap<String, String>... params) {
				Log.d(TAG, "Starting download of : " + params[0].get(Track.key_download_url));
				
				byte[] buf = new byte[4096]; // 4KB buffer
				int count; 
				BufferedInputStream bis;
				FileOutputStream fos;
				int position;
				int length;
				
				boolean success = true;
				publishProgress(R.string.cloud_downloader_event_title_track, 0, -1);
				final String filename = params[0].get(Track.key_local_play_url);
				
				try {
					
					URL url;
					InputStream is;
					URLConnection conn;
					int contentLength;
					
					HttpClient httpClient = new DefaultHttpClient();
					
					String consumerKey = ((SoundCloudApplication) CloudDownloaderService.this.getApplication()).getConsumerKey();
					String audio = params[0].get(Track.key_download_url);
					String waveform = params[0].get(Track.key_waveform_url);
					String artwork = params[0].get(Track.key_artwork_url);
					String avatar = params[0].get(Track.key_user_avatar_url);
					

					Log.d(TAG, "Downloading from location : " + audio);
					Log.d(TAG, "Downloading artwork from location : " + artwork);
					Log.d(TAG, "Downloading avatar from location : " + avatar);
					
//					if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED){
//						audio = mCloudComm.signStreamUrl(audio);
//						waveform = mCloudComm.signStreamUrl(waveform);
//						artwork = mCloudComm.signStreamUrl(artwork);						
//					}
					
					
					url = new URL(audio + "?consumer_key=" + consumerKey);
					is = (InputStream) url.getContent();
					
					conn = url.openConnection();
					
					contentLength = conn.getContentLength();
					
					
					
					
					
					Boolean _downloaded = false;
					File outFile = new File(filename);
					if (outFile.exists()){
						if (outFile.length() == contentLength){
							_downloaded = true;
						}
					}

					int _downloadPercentage = -1;
					int _lastDownloadPercentage = -1;
					
					if (!_downloaded){
						Log.d(TAG, "Downloading with input stream : " + is);
						Log.d(TAG, "Downloading to : " + filename);
						bis = new BufferedInputStream(is);
						fos = new FileOutputStream(filename);
						count = 0;
						
						
						position = 0;
						length = contentLength;
						
						while ((count = bis.read(buf)) >= 0) {
							//Log.d(TAG, "Downloading reading : " + count);
							//Log.d(TAG, "Downloading writing " + buf + " to : " + fos);
							fos.write(buf, 0, count);
							//Log.d(TAG, "Downloading wrote to fos : " + fos);
							position += count;
							_downloadPercentage = (int)((((float) position)/((float)length))*100);
							
							if (_lastDownloadPercentage != _downloadPercentage){
								publishProgress(R.string.cloud_downloader_event_title_track, position, length);
								_lastDownloadPercentage = _downloadPercentage;
							}
							
							if (isCancelled()) {
								Log.d(TAG, "Download cancelled!");
								success = false;
								break;
							}
						//	Log.d(TAG, "Loop finished : " + bis);
						}
						Log.d(TAG, "Downloading finished");
						
						fos.flush();
						fos.close();
						bis.close();
					}
					
					
					if (!isCancelled() && artwork != null && !artwork.contentEquals("")) {
						_downloadPercentage = -1;
						_lastDownloadPercentage = -1;
						publishProgress(R.string.cloud_downloader_event_title_artwork, -1, -1);
						
						Log.d(TAG, "Downloading artwork");
						
						String artworkPath = params[0].get(Track.key_local_artwork_url);
						File artworkFile = new File(artworkPath);
						artworkFile.createNewFile();
						position = 0;
						
						
//						if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED){
//							is = mCloudComm.getContent(artwork);
//							contentLength = (int) mCloudComm.getHttpResponse(artwork).getEntity().getContentLength();
//						} else {
							url = new URL(artwork + "?consumer_key=" + consumerKey);
							is = (InputStream) url.getContent();
							conn = url.openConnection();
							contentLength = conn.getContentLength();
//						}
						
						bis = new BufferedInputStream(is);
						fos = new FileOutputStream(artworkFile);
						
						
						while ((count = bis.read(buf)) >= 0) {
							position += count;
							fos.write(buf, 0, count);
							_downloadPercentage = (int)((((float) position)/((float)contentLength))*100);
							if (_lastDownloadPercentage != _downloadPercentage){
								publishProgress(R.string.cloud_downloader_event_title_artwork, position, contentLength);
								_lastDownloadPercentage = _downloadPercentage;
							}
						}
						
						fos.flush();
						fos.close();
						bis.close();
						
					}
					if (!isCancelled()) {
						publishProgress(R.string.cloud_downloader_event_title_waveform, -1, -1);
						_downloadPercentage = -1;
						_lastDownloadPercentage = -1;
						
						Log.d(TAG, "Downloading waveform");
						
						String waveformPath = params[0].get(Track.key_local_waveform_url);
						File waveformFile = new File(waveformPath);
						waveformFile.createNewFile();
						position = 0;
						
//						if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED){
//							is = mCloudComm.getContent(waveform);
//							contentLength = (int) mCloudComm.getHttpResponse(waveform).getEntity().getContentLength();
//						} else {
							url = new URL(waveform + "?consumer_key=" + consumerKey);
							is = (InputStream) url.getContent();
							conn = url.openConnection();
							contentLength = conn.getContentLength();
//						}
						
						bis = new BufferedInputStream(is);
						fos = new FileOutputStream(waveformFile);
						
						while ((count = bis.read(buf)) >= 0) {
							position += count;
							fos.write(buf, 0, count);
							_downloadPercentage = (int)((((float) position)/((float)contentLength))*100);
							if (_lastDownloadPercentage != _downloadPercentage){
								publishProgress(R.string.cloud_downloader_event_title_waveform, position, contentLength);
								_lastDownloadPercentage = _downloadPercentage;
							}
						}
						
						fos.flush();
						fos.close();
						bis.close();
						
					}
					if (!isCancelled() && !avatar.contentEquals("") && CloudUtils.checkIconShouldLoad(avatar)) {
						publishProgress(R.string.cloud_downloader_event_title_avatar, -1, -1);
						
						String avatarPath = CloudUtils.buildLocalAvatarUrl(params[0].get(Track.key_user_permalink));
						File avatarFile = new File(avatarPath);
						avatarFile.createNewFile();
						position = 0;
						
//						if (mCloudComm.getState() == SoundCloudAPI.State.AUTHORIZED){
//							is = mCloudComm.getContent(avatar);
//							contentLength = (int) mCloudComm.getHttpResponse(avatar).getEntity().getContentLength();
//						} else {
							url = new URL(avatar + "?consumer_key=" + consumerKey);
							is = (InputStream) url.getContent();
							conn = url.openConnection();
							contentLength = conn.getContentLength();
//						}
						
						bis = new BufferedInputStream(is);
						fos = new FileOutputStream(avatarFile);
						
						while ((count = bis.read(buf)) >= 0) {
							position += count;
							fos.write(buf, 0, count);
							publishProgress(R.string.cloud_downloader_event_title_avatar, position, contentLength);
						}
						
						fos.flush();
						fos.close();
						bis.close();
						
					}
					success = success && !isCancelled();
				} catch (MalformedURLException e) {
					Log.d(TAG, "Malformed URL exception: " + e.toString());
					e.printStackTrace();
					success = false;
				} catch (IOException e) {
					Log.d(TAG, "IO exception: " + e.toString());
					e.printStackTrace();
					success = false;
				} finally {
					
					Log.d(TAG, "Terminating with status: " + success);
				}
				return success;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				
				Log.i(TAG,"Mark track as downloaded " + result);
				db = new DBAdapter(getApplicationContext());
				db.open();
				if (result){
					db.markTrackDownloaded(mDownloadingData.getData(Track.key_id));
					mDownloadingData.putData(Track.key_download_status, Track.DOWNLOAD_STATUS_DOWNLOADED);
				} else {
					db.markTrackDownloadError(mDownloadingData.getData(Track.key_id), "true");
					mDownloadingData.putData(Track.key_download_error, "true");
					notifyChange(DOWNLOAD_ERROR);
				}
				db.close();

				notifyChange(DOWNLOAD_FINISHED);
				
				nextTrackInDownloadList();
			}

		
		}
		
		private void gotoIdleState(){
			stopForeground(true);
		}
	    
	    
	  
		
	    
	    private void showDownloadNotificationProgress(int eventStringID, int percentage) {
	    	  //CharSequence titleText = getString(R.string.cloud_downloader_event_title_track);
			 CharSequence trackText = mDownloadingData.getData(User.key_username).toString() + " - " + mDownloadingData.getData(Track.key_title).toString();
			 CharSequence percentageText = String.format(getString(R.string.cloud_downloader_event_download_avatar), percentage);
	         
	         //notificationView.setTextViewText(R.id.title, titleText);
	         notificationView.setTextViewText(R.id.trackname, trackText);
	         notificationView.setTextViewText(R.id.percentage, percentageText);
	         notificationView.setProgressBar(R.id.progress_bar, 100, percentage, false);
	    	
	    	// Send the notification.
	        nm.notify(DOWNLOAD_NOTIFY_ID, mNotification);
	    }

	
	    /*
	     * By making this a static class with a WeakReference to the Service, we
	     * ensure that the Service can be GCd even when the system process still
	     * has a remote reference to the stub.
	     */
	    static class ServiceStub extends ICloudDownloaderService.Stub {
	        WeakReference<CloudDownloaderService> mService;
	        
	        ServiceStub(CloudDownloaderService service) {
	            mService = new WeakReference<CloudDownloaderService>(service);
	        }
	       
	        public int getCurrentDownloadPercentage() {
	            return mService.get().getCurrentDownloadPercentage();
	        }

			public Track getCurrentDownloadingTrackInfo() throws RemoteException {
				return mService.get().getDownloadingData();
			}

			@SuppressWarnings("unchecked")
			public void downloadTrack(Track trackdata) throws RemoteException {
				mService.get().addTrackToDownloadQueue(trackdata);
				
			}

			public String getCurrentDownloadId() throws RemoteException {
				return mService.get().getCurrentDownloadId();
			}
	    }
	    
	    private final IBinder mBinder = new ServiceStub(this);

}//end class MyService
