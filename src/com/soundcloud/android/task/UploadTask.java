package com.soundcloud.android.task;

import java.io.File;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.content.FileBody;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.soundcloud.android.CloudCommunicator;
import com.soundcloud.utils.http.ProgressListener;

public class UploadTask extends AsyncTask<String, Integer, Boolean> implements ProgressListener {

	private static final String TAG = "VorbisEncoderTask";
	
	private boolean reporting;
	
	public Context context;
	public File trackFile = null;
	public File artworkFile = null;
	public List<NameValuePair> trackParams;
	
	public long transferred;
	private long totalTransfer;
	

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
	}

	@Override
	protected void onPostExecute(Boolean result) {
	}

	@Override
	protected Boolean doInBackground(String... params) {
		
		final FileBody trackBody = new FileBody(trackFile);
		final FileBody artworkBody = artworkFile == null ? null : new FileBody(artworkFile);
		
		reporting = true;
		
		totalTransfer = (artworkFile == null ? trackFile.length() : trackFile.length() + artworkFile.length());
		
		ProgressListener transferListener = new ProgressListener(){
			@Override
			public void transferred(long amount) {
				
			}
			
		};
		
		final Thread uploadThread = new Thread(new Runnable()
  	   {
  	       public void run()
               {
                   try
                   {
                	   if (artworkBody == null)
                		   CloudCommunicator.getInstance(context).upload(trackBody, trackParams, UploadTask.this);
                	   else
                		   CloudCommunicator.getInstance(context).upload(trackBody, artworkBody, trackParams, UploadTask.this);
                   } catch (Exception e) {
                           e.printStackTrace();
                   }
               }
  	       });
  	   
  	  uploadThread.start();
  	  
  	while(uploadThread.isAlive()){
  		Log.i("TRANSFERRING",transferred + " of " + totalTransfer);
  		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
  	}
  	
  	reporting = false;
  	
	if (isCancelled()) return false;
  	
  	return true;

  	}

	@Override
	public void transferred(long amount) {
		transferred = amount;
	}

}
