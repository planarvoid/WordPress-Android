package com.soundcloud.android.task;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.AsyncTask;
import android.util.Log;

import com.soundcloud.utils.record.WaveHeader;

public class WavEncoderTask extends AsyncTask<String, Integer, Boolean> {
	public File pcmFile;
	public File wavFile;
	
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
		
		if (params.length < 2){
			System.out.println( "Incorrect number of parameters. Input file and output file required" );
			return false;
		}
		
		pcmFile = new File(params[0]);
		wavFile = new File(params[1]);
		
		//FileChannel source = null;
		//FileChannel dest = null;
		

    	  int musicLength = (int)(pcmFile.length());
    	  
    	  //publishProgress(0, musicLength);
    	  
    	  try {
    		  
    		 OutputStream out = new FileOutputStream(wavFile);
    		 
            try {
            	
                WaveHeader hdr = new WaveHeader(WaveHeader.FORMAT_PCM,
                        (short)2, 44100, (short)16, musicLength*2);
                hdr.write(out);
                
                // Create a DataInputStream to read the audio data back from the saved file.
	    	    InputStream is = new FileInputStream(pcmFile);
	    	    BufferedInputStream bis = new BufferedInputStream(is);
	    	    DataInputStream dis = new DataInputStream(bis);
	    	  
	    	    // read/write in chunks
	    	    int i = 0;
	    	    int j = 0;
	    	    int chunkSize = 32768;
	    	    byte[] music = new byte[chunkSize];
	    	    while (dis.available() > 0) {
	    	    	j = i % chunkSize;
	    	    	if (j == 0){
	    	    		if (i > 0)
	    	    			out.write (music);
	    	    		publishProgress(i, musicLength*2);
	    	    	}
	    	    	
	    	    	//music[j] = dis.readByte(); 
	    	    	music[j] = music[j + 2] = dis.readByte();
	    	    	if (dis.available() > 0) {
			    	     music[j+1] = music[j + 3] = dis.readByte();
			    	}
			    	
	    	      i=i+4;
	    	    }
	    	    
	    	    publishProgress(musicLength*2, musicLength*2);
	    	    
	    	    // Close the input streams.
	    	    dis.close();
    	    
            } finally {
                out.close();
            }
            
            return true;

    	  } catch (Throwable t) {
    		  System.out.println( "Wav Processing Failure"  + t.toString());
    	  }
    	  
    	  return false;
    	  
    	 
		
	}


}
