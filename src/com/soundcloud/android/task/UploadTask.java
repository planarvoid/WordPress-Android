package com.soundcloud.android.task;

import java.io.File;
import java.util.List;

import org.apache.http.NameValuePair;
import org.urbanstew.soundcloudapi.ProgressFileBody;

import android.content.Context;
import android.os.AsyncTask;

import com.soundcloud.android.CloudCommunicator;

public class UploadTask extends AsyncTask<String, Integer, Boolean> {

	private static final String TAG = "VorbisEncoderTask";

	public Context context;
	public File trackFile;
	public File artworkFile;
	public List<NameValuePair> trackParams;

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
		
		final ProgressFileBody trackBody = new ProgressFileBody(trackFile);
		
		final Thread uploadThread = new Thread(new Runnable()
  	   {
  	       public void run()
               {
                   try
                   {
                	   CloudCommunicator.getInstance(context).getApi().upload(trackBody, trackParams);
                   } catch (Exception e) {
                           e.printStackTrace();
                   }
               }
  	       });
  	   
  	   uploadThread.start();
		 

          	  while(uploadThread.isAlive()){
          		publishProgress((int) trackBody.getBytesTransferred(), (int) trackBody.getContentLength());
	           		 try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
	           	   }
          	  return true;
          	 

  	}

}
