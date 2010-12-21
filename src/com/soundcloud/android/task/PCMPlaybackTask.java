package com.soundcloud.android.task;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.view.ScCreate.CreateState;

public class PCMPlaybackTask extends AsyncTask<String, Integer, Boolean> {
	private File mPCMFile;
	private Boolean mPlaying;
	
	private PlaybackListener listener = null;
	private Thread writeThread;
	
	public AudioTrack playbackTrack;
	public int minSize;
	
	
	public PCMPlaybackTask(File pcmFile){
		mPCMFile = pcmFile;
		
		
		   
	}
	
	public void setPlaybackListener(PlaybackListener playbackListener){
		listener = playbackListener;
	}
	
	public void stopPlayback(){
		mPlaying = false;
		
		if (playbackTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
			playbackTrack.pause();
		
		if (writeThread.isAlive())
			writeThread.interrupt();
	}

	@Override
	protected void onPreExecute() {
		mPlaying = true;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		if (listener != null)
			listener.onPlayProgressUpdate(progress[0]);
		
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (writeThread.isAlive())
			writeThread.interrupt();
		
		playbackTrack.release();
		
		if (listener != null)
			listener.onPlayComplete(result);
	}

	@Override
	protected Boolean doInBackground(String... params) {
		try {
			final int minSize =AudioTrack.getMinBufferSize( 44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT );        
	  		playbackTrack = new AudioTrack( AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
	  		playbackTrack.play();
	  		
	  		
	  		writeThread = new Thread(new Runnable()
	   	   {
	   	       public void run()
	                {
			   	    	try {
			   	    		
			   	    		InputStream is = new FileInputStream(mPCMFile);
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
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                }
	   	   });
	  		writeThread.start();
	  		
			
	  		
			while (mPlaying && playbackTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING){
				publishProgress(playbackTrack.getPlaybackHeadPosition()*2);
				try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
			}
			
			if (playbackTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
				playbackTrack.stop();
  		    
			return true;
			
		} catch (Exception e){
			return false;
		}
	}


	// Define our custom Listener interface
	public interface PlaybackListener {
		public abstract void onPlayProgressUpdate(int position);
		public abstract void onPlayComplete(boolean result);
	}
	
}





/* UNUSED, For later implementation
 * private int playbackPosition;
	private long mLastSeekEventTime;
    
	private boolean mFromTouch = false;
	private boolean mDragging = false;
    
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			mLastSeekEventTime = 0;
			mFromTouch = true;
			mDragging = true;
			
			if (!isPlayingBack) {
				mCurrentState = CreateState.playback;
				activateState();
			}
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			
			if (!fromuser) {
				return;
			}
			
			long now = SystemClock.elapsedRealtime();
			if (now - mLastSeekEventTime > 250) {
				mLastSeekEventTime = now;
				playbackTrack.setPlaybackHeadPosition(progress);
			}
		}

		public void onStopTrackingTouch(SeekBar bar) {
			mFromTouch = false;
			mDragging = false;
		}
	};
	*/
