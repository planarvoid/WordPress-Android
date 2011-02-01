package com.soundcloud.android.task;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import com.soundcloud.utils.record.WaveHeader;

public class PCMPrepareTask extends AsyncTask<String, Integer, Boolean> {
	public File pcmFile;
	public AudioTrack playbackTrack;
	public int minSize;
	
	public interface stages {
		int reading = 1;
		int writing = 2;
	}

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
		try {
        	InputStream is = new FileInputStream(pcmFile);
  		    BufferedInputStream bis = new BufferedInputStream(is);
  		    DataInputStream dis = new DataInputStream(bis);

  		    byte[] buffer = new byte[minSize];
  		    while (dis.available() > 0) {
  		    	int bytesRead = 0;
  		    	while(dis.available() > 0 && bytesRead < minSize){
  		    		buffer[bytesRead] = dis.readByte();
  		    		bytesRead++;
  		    	}
  		    	playbackTrack.write(buffer, 0, bytesRead );
  		    }
  		    
  		    Log.i("ScCreate","prepare is done");
  		    
    	 return true;
		} catch (Exception e){
			return false;
		}
	}


}
