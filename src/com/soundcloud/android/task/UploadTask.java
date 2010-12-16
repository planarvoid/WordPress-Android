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
	public File trackFile = null;
	public File artworkFile = null;
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
		final ProgressFileBody artworkBody = artworkFile == null ? null : new ProgressFileBody(artworkFile);
		
		final Thread uploadThread = new Thread(new Runnable()
  	   {
  	       public void run()
               {
                   try
                   {
                	   if (artworkBody == null)
                		   CloudCommunicator.getInstance(context).upload(trackBody, trackParams);
                	   else
                		   CloudCommunicator.getInstance(context).upload(trackBody, artworkBody, trackParams);
                   } catch (Exception e) {
                           e.printStackTrace();
                   }
               }
  	       });
  	   
  	   uploadThread.start();
  	   
  	  while(uploadThread.isAlive()){
  		  if (artworkBody == null)
  			  publishProgress((int) trackBody.getBytesTransferred(), (int) trackBody.getContentLength());
  		  else
  			  publishProgress((int) trackBody.getBytesTransferred() + (int) artworkBody.getBytesTransferred(), (int) trackBody.getContentLength() + (int) artworkBody.getContentLength());
  		  
       		 try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
      }
  	  return true;
          	 

  	}

}
