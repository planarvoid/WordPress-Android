package com.soundcloud.android;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.urbanstew.soundcloudapi.ProgressFileBody;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.soundcloud.utils.WaveHeader;
public class ScCreate extends ScTabView {


	


	// Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "ScCreate";
   
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // Our power manager.
    private PowerManager powerManager = null;

    // The surface manager for the view.
    private SCRecorder mRecorder = null;
    
    private ImageButton btnAction;
    private Button btnReset;
    private Button btnUpload;
    private TextView txtInstructions;
    private LinearLayout mFileLayout;
    private FrameLayout mGaugeHolder;
    private ProgressBar mProgressBar;
    
    private File mRecordFile;
    private File mWavFile;
    
    private LazyActivity mActivity;
    
    private CreateState mCurrentState;
    private Chronometer mChronometer;
    
	public enum CreateState { idle_record, record, idle_playback, playback, idle_upload, upload }
    
	MediaPlayer mMediaPlayer;
	private int playbackPosition;
	private Boolean isPlayingBack;
	private Handler mHandler = new Handler();
	

    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
     * Called when the activity is starting.  This is where most
     * initialisation should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of
     * the activity lifecycle executing.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     * 
     * @param   icicle          If the activity is being re-initialised
     *                          after previously being shut down then this
     *                          Bundle contains the data it most recently
     *                          supplied in onSaveInstanceState(Bundle).
     *                          Note: Otherwise it is null.
     */
   
    
    public ScCreate(LazyActivity activity) {
		super(activity);
		
		mActivity = activity;
		
		LayoutInflater inflater = (LayoutInflater) activity
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.sc_record, this);
		
		mFileLayout = (LinearLayout) findViewById(R.id.file_layout);
		mGaugeHolder = (FrameLayout) findViewById(R.id.gauge_holder);
		txtInstructions = (TextView) findViewById(R.id.txt_instructions);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mChronometer = (Chronometer) findViewById(R.id.chronometer);
		mChronometer.setVisibility(View.GONE);
		
		btnAction = (ImageButton) findViewById(R.id.btn_action);
		btnAction.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onAction();
			}
		});
		
		btnReset = (Button) findViewById(R.id.btn_reset);
		btnReset.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mCurrentState = CreateState.idle_record;
				activateState();
			}
		});
		
		btnUpload = (Button) findViewById(R.id.btn_upload);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mCurrentState = CreateState.upload;
			}
		});
		
		

		
		mCurrentState = CreateState.idle_record;
		
    	mRecorder = new SCRecorder(mActivity);
    	mRecorder.setBackgroundColor(0xFFFFFF);
		mRecorder.setLayoutParams(new LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,(int) (50*mActivity.getResources().getDisplayMetrics().density)));
		mRecorder.setSampleRate(44100);
		mRecorder.setBlockSize(1024);
		mGaugeHolder.addView(mRecorder);
		mRecorder.onStart();
		
		/*
		btnRecord = (Button) findViewById(R.id.btn_record);
		btnRecord.setOnClickListener(mRecordBtnListener);
		
		btnPlayback = (Button) findViewById(R.id.btn_playback);
		btnPlayback.setOnClickListener(mPlaybackBtnListener);
		
		btnUpload = (Button) findViewById(R.id.btn_upload);
		btnUpload.setOnClickListener(mUploadListener);*/
		
		
	}
    
    private void onAction(){
    	switch (mCurrentState){
    		case idle_record:
    			mCurrentState = CreateState.record;
    			break;
    			
    		case record:
    			mCurrentState = CreateState.idle_playback;
    			break;
    			
    		case idle_playback:
    			mCurrentState = CreateState.playback;
    			break;
    			
    		case playback:
    			mCurrentState = CreateState.playback;
    			break;
    	}
    	
    	activateState();
    }
    
    private void activateState(){
    	activateState(true);
    }
    
    private void activateState(Boolean takeAction){
    	switch (mCurrentState){
			case idle_record:
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_states));
				mFileLayout.setVisibility(View.GONE);
				mChronometer.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.GONE);
				txtInstructions.setVisibility(View.VISIBLE);
				attachRecorder();
				break;
				
			case record:
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_stop_states));
				txtInstructions.setVisibility(View.GONE);
				mChronometer.setBase(SystemClock.elapsedRealtime());
				mChronometer.setVisibility(View.VISIBLE);
				
				if (takeAction) startRecording();
				break;
			case idle_playback:
				if (takeAction) stopRecording();
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_play_states));
				removeRecorder();
				mFileLayout.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.VISIBLE);
				
				
				break;
			case playback:
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_pause_states));
				if (takeAction) startPlayback();
				
				break;
			case upload:
				//btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_pause_states));
				startUpload();
				mCurrentState = CreateState.idle_record;
				activateState();
				break;	
				
				
		}
    }
    
    private void attachRecorder(){
    	if (mRecorder.getParent() != mGaugeHolder) mGaugeHolder.addView(mRecorder);
        mRecorder.onResume();
        mRecorder.surfaceStart();

    }
    
    private void removeRecorder(){
    	mRecorder.surfaceStop();
    	mRecorder.onPause();
    	mGaugeHolder.removeView(mRecorder);
    	
    }
    
   
    
    private void startRecording(){
    	Log.i(TAG,"Starting recording");
    	
	  	mRecordFile = new File(Environment.getExternalStorageDirectory() + "/temp_rec_44100.pcm" );
	  	mWavFile = new File(Environment.getExternalStorageDirectory() + "/temp_wav_44100.wav" );
		
		Log.i(TAG,"Recording to " + mRecordFile.toString());
		
		mRecorder.startRecording(mRecordFile);
		mChronometer.start();
    }
    
    private void stopRecording(){
    	Log.i(TAG,"Stopping recording");
    	
		mRecorder.stopRecording();
		mChronometer.stop();
		
		process();
    }
    
    private void process(){
		Log.i(TAG,"Processing" + mRecordFile);
    	if (mRecordFile == null)
    		return;

    	
    	
		
    	
    	  // Get the length of the audio stored in the file (16 bit so 2 bytes per short)
    	  // and create a short array to store the recorded audio.
    	  int musicLength = (int)(mRecordFile.length());
    	  byte[] music = new byte[musicLength];


    	  try {
    		
    	    // Create a DataInputStream to read the audio data back from the saved file.
    	    InputStream is = new FileInputStream(mRecordFile);
    	    BufferedInputStream bis = new BufferedInputStream(is);
    	    DataInputStream dis = new DataInputStream(bis);
    	    
    	    
    	    // Read the file into the music array.
    	    int i = 0;
    	    while (dis.available() > 0) {
    	      music[i] = dis.readByte();
    	      i++;
    	    }

    	 
    	    // Close the input streams.
    	    dis.close();     
    	
    	    OutputStream out = new FileOutputStream(mWavFile);
            try {
              
                WaveHeader hdr = new WaveHeader(WaveHeader.FORMAT_PCM,
                        (short)1, 44100, (short)16, musicLength);
                hdr.write(out);
                out.write(music);
            }
            finally {
                out.close();
            }
            
            Log.e(TAG,"Processed " + mRecordFile.length());

    	  } catch (Throwable t) {
    	    Log.e(TAG,"Processing Failed " + t.toString());
    	  }
    	  
    	  mRecordFile.delete();

    	}
    
    private void startPlayback(){
    	if (mWavFile == null)
    		return;
    	

    	
    	mMediaPlayer = new MediaPlayer();
    	try {
        	FileInputStream fis = new FileInputStream(mWavFile);
			mMediaPlayer.setDataSource(fis.getFD());
	        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	        mMediaPlayer.setOnPreparedListener(preparedlistener);
	    	mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
	    	mMediaPlayer.setOnCompletionListener(completeListener);
	        mMediaPlayer.prepare();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

       //	mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
       //	mMediaPlayer.setOnSeekCompleteListener(seeklistener);
    	
		
    	}
    
    
    MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
        	Log.i(TAG,"MEDIA PLAYER PREPARED ");
        	mProgressBar.setMax(mMediaPlayer.getDuration()); 
        	Log.i(TAG,"MAXIMUM " + mMediaPlayer.getDuration());
        	mMediaPlayer.start();
        	
        	isPlayingBack = true;
        	 // Start lengthy operation in a background thread
            new Thread(new Runnable() {
                public void run() {
                    while (isPlayingBack) {
                    	
                    	playbackPosition = mMediaPlayer.getCurrentPosition();
                    	Log.i(TAG,"POSITION " + playbackPosition + " "  + mMediaPlayer.getDuration());
                    	
                        // Update the progress bar
                        mHandler.post(new Runnable() {
                            public void run() {
                                mProgressBar.setProgress(playbackPosition);
                            }
                        });
                    }
                }
            }).start();
            //play();
            /*if (mPlayFromTime != 0){
                mPlayer.seek(mPlayFromTime);
                mPlayFromTime = 0;
            }*/
            
        }
    };
    
    MediaPlayer.OnBufferingUpdateListener bufferinglistener = new MediaPlayer.OnBufferingUpdateListener() {
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			Log.i(TAG,"MP On Buffer " + percent);
			//mLoadPercent = percent;
		}
	};
	
	MediaPlayer.OnSeekCompleteListener seeklistener = new MediaPlayer.OnSeekCompleteListener() {
		public void onSeekComplete(MediaPlayer mp) {
			 Log.i(TAG,"Media player seek complete");
		}
	};


    MediaPlayer.OnCompletionListener completeListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
        	isPlayingBack = false;
        	mProgressBar.setProgress(0);
        	mCurrentState = CreateState.idle_playback;
        	activateState(false);
          Log.i(TAG,"Media player complete");
        }
    };
    
    private void startUpload(){
    	
    	final List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();
    	 params.add(new BasicNameValuePair("track[title]", "record_test_"+Math.round(Math.random()*10000)));
    	 params.add(new BasicNameValuePair("track[sharing]", "private"));
    	 final ProgressFileBody fileBody = new ProgressFileBody(mWavFile);
    	   
    	   Thread progressThread = new Thread(new Runnable()
    	   {
    	       public void run()
    	               {
    	                       try
    	                       {
    	                    	   mActivity.mCloudComm.getApi().upload(fileBody, params);
    	                       } catch (Exception e)
    	                       {
    	                               e.printStackTrace();
    	                       }
    	               }
    	       });
    	   
    	   progressThread.start();
    	   while(progressThread.isAlive()){
    		   Log.i(TAG,"Transferring file " + fileBody.getBytesTransferred() + " of " + mWavFile.length());
    	   }

    	   Log.i(TAG,"File Done");
    	   mCurrentState = CreateState.idle_record;
    	   activateState();
    	  
    	}
    
   
    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */

    @Override
	public void onStart() {
    	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
    	activateState();
    	
    }


    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.  This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */

    protected void onResume() {

        
    }



    protected void onPause() {



    }

    @Override
    public void onStop() {
    	mRecorder.surfaceStop();
    	mRecorder.onPause();
    	mRecorder.onStop();
    	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); 
    }
    
   

   
    
    
    
	
}
