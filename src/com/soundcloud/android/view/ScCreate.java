package com.soundcloud.android.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.urbanstew.soundcloudapi.ProgressFileBody;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.CloudUtils.Dialogs;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.activity.Main;
import com.soundcloud.android.task.VorbisEncoderTask;
import com.soundcloud.utils.AnimUtils;
import com.soundcloud.utils.PowerGauge;
import com.soundcloud.utils.Recorder;
public class ScCreate extends ScTabView {


	


	// Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "ScCreate";
   
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    private Recorder mRecorder = null;
    private ViewFlipper mViewFlipper;

    private TextView txtInstructions;
    private LinearLayout mFileLayout;
    private FrameLayout mGaugeHolder;
    private PowerGauge mPowerGauge;
    private SeekBar mProgressBar;
    
    private RadioGroup mRdoPrivacy;
    private EditText mWhereText;
    private EditText mWhatText;
    private TextView mArtworkInstructions;
    private ImageView mArtwork;
    
    private ImageButton btnAction;
    private Button btnReset;
    private Button btnCancel;
    private Button btnSave;
    private Button btnUpload;
    
    private File mRecordFile;
    private File mOggFile;
    private String mArtworkUri;
    
    private LazyActivity mActivity;
    
    private CreateState mLastState;
    private CreateState mCurrentState;
    private Chronometer mChronometer;
    
	public enum CreateState { idle_record, record, idle_playback, playback, idle_upload, upload }
    
	MediaPlayer mMediaPlayer;
	
	private Boolean isPlayingBack;
	private Handler mHandler = new Handler();
	
	private Thread uploadThread;
	

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
		inflater.inflate(R.layout.sc_create, this);
		
		mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);
		
		mFileLayout = (LinearLayout) findViewById(R.id.file_layout);
		mGaugeHolder = (FrameLayout) findViewById(R.id.gauge_holder);
		txtInstructions = (TextView) findViewById(R.id.txt_instructions);
		mProgressBar = (SeekBar) findViewById(R.id.progress_bar);
		mChronometer = (Chronometer) findViewById(R.id.chronometer);
		mChronometer.setVisibility(View.GONE);
		mProgressBar.setOnSeekBarChangeListener(mSeekListener);

		
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
		
		btnSave = (Button) findViewById(R.id.btn_save);
		btnSave.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mCurrentState = CreateState.idle_upload;
				activateState();
			}
		});
		
		btnCancel = (Button) findViewById(R.id.btn_cancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mCurrentState = CreateState.idle_playback;
				activateState();
			}
		});
		
		btnUpload = (Button) findViewById(R.id.btn_upload);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mCurrentState = CreateState.upload;
				activateState();
			}
		});
		
		
		mArtwork = (ImageView) findViewById(R.id.artwork);
		mArtworkInstructions = (TextView) findViewById(R.id.txt_artwork_instructions);
		mWhatText = (EditText) findViewById(R.id.what);
		mWhereText = (EditText) findViewById(R.id.where);
		
		mRdoPrivacy = (RadioGroup) findViewById(R.id.rdo_privacy);
		
		mArtwork.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				mActivity.startActivityForResult(intent, CloudUtils.GALLERY_IMAGE_PICK_CODE);
			}
		});
		
		mArtwork.setOnLongClickListener(new OnLongClickListener() {
		    @Override
		    public boolean onLongClick(View v) {
		        clearArtwork();
		        return true;
		    }
		});
		
		mCurrentState = CreateState.idle_record;
		
    	mRecorder = new Recorder(mActivity);
    	mPowerGauge = new PowerGauge(mActivity);
    	mRecorder.setPowerGauge(mPowerGauge);
    	mGaugeHolder.addView(mPowerGauge);
    	
    	

    	mMediaPlayer = new MediaPlayer();
    	try {
	        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	        mMediaPlayer.setOnPreparedListener(preparedlistener);
	    	mMediaPlayer.setOnBufferingUpdateListener(bufferinglistener);
	    	mMediaPlayer.setOnCompletionListener(completeListener);
	        
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		restoreRecordings();
	}
    
    @Override
	public void onStart() {
    	super.onStart();
    	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    	
    	if (!mOggFile.exists()){
    		  mCurrentState = CreateState.idle_record;
    	} else {
    		preparePlayback();
    	}
    	
    	activateState();
    	
    }
    
    @Override
    public void onStop() {
    	mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); 
    }
    
    
    public void setPickedImage(String imageUri){
    	mArtworkUri = imageUri;
    	mArtwork.setImageBitmap(BitmapFactory.decodeFile(mArtworkUri));
    	mArtworkInstructions.setText(mActivity.getResources().getString(R.string.record_artwork_instructions_added));
    }
    
    public void clearArtwork(){
    	mArtworkUri = null;
    	mArtwork.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.artwork_player));
    	mArtworkInstructions.setText(mActivity.getResources().getString(R.string.record_artwork_instructions));
    }
    
    
    
    
    private void restoreRecordings(){
    	mRecordFile = new File(Environment.getExternalStorageDirectory() + "/temp_rec_44100.pcm" );
	  	mOggFile = new File(Environment.getExternalStorageDirectory() + "/temp_ogg.ogg");
	  	
	  	//reset the pcm and wav files just in case they are taking up space
	  	clearSourceFiles();
	  	
	  	if (mOggFile.exists()){
	  		
	  		mCurrentState = CreateState.idle_playback;
	  		activateState();
	  	} 
	  	
		
    }
    
    private void clearRecording(){
    	clearSourceFiles();
    	if (mOggFile.exists()) mOggFile.delete();
    }
    
    private void clearSourceFiles(){
    	if (mRecordFile.exists()) mRecordFile.delete();
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
    			mCurrentState = CreateState.idle_playback;
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
				flipLeft();
				
				mWhereText.setText("");
				mWhatText.setText("");
				clearArtwork();
				
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_states));
				mFileLayout.setVisibility(View.GONE);
				mChronometer.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.GONE);
				mPowerGauge.setVisibility(View.GONE);
				txtInstructions.setVisibility(View.VISIBLE);
				mRecorder.deactivate();
				
				break;
				
			case record:
				flipLeft();
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_stop_states));
				txtInstructions.setVisibility(View.GONE);
				mChronometer.setBase(SystemClock.elapsedRealtime());
				mChronometer.setVisibility(View.VISIBLE);
				mPowerGauge.setVisibility(View.VISIBLE);
				
				if (takeAction)
					startRecording();
				
				break;
				
			case idle_playback:
				flipLeft();
				if (takeAction) {
					if (mLastState == CreateState.record) stopRecording();
					if (mLastState == CreateState.playback) pausePlayback();
				}
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_play_states));
				txtInstructions.setVisibility(View.GONE);
				mFileLayout.setVisibility(View.VISIBLE);
				mChronometer.setVisibility(View.GONE);
				mPowerGauge.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.VISIBLE);
				break;
				
			case playback:
				flipLeft();
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_pause_states));
				if (takeAction) 
					startPlayback();
				break;
				
			case idle_upload:
				flipRight();
				break;	
			
			case upload:
				flipRight();
				startUpload();
				break;	
				
				
		}
    	
    	mLastState = mCurrentState;
    	btnAction.setEnabled(true);
    }
    
   
   private void flipRight(){
	   if (mViewFlipper.getDisplayedChild() == 1)
		   return;
	   
	   mViewFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
	   mViewFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
	   mViewFlipper.setDisplayedChild(1);
   }
   
   private void flipLeft(){
	   if (mViewFlipper.getDisplayedChild() == 0)
		   return;
	   
	   mViewFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
	   mViewFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
	   mViewFlipper.setDisplayedChild(0);
   }
    
    private void startRecording(){
    	Log.i(TAG,"Starting recording");
    	
    	mRecorder.activate();
		mRecorder.startRecording(mRecordFile);
		mChronometer.start();
    }
    
    private void stopRecording(){
    	
    	//disable actions during processing and playback preparation
    	btnAction.setEnabled(false);
    	
		mRecorder.stopRecording();
		mChronometer.stop();
		
		mRecorder.deactivate();
		
		/*EncodePCMTask processTask = new EncodePCMTask();
		processTask.activity = mActivity;
		processTask.createRef = this;
		processTask.execute(mRecordFile.getAbsolutePath(), mWavFile.getAbsolutePath());*/
		
		EncodeVorbisTask processTask = new EncodeVorbisTask();
		processTask.activity = mActivity;
		processTask.createRef = this;
		processTask.waveFileSize = (int) mRecordFile.length();
		processTask.execute(mRecordFile.getAbsolutePath(), mOggFile.getAbsolutePath());
    }
    
   
    private void preparePlayback(){
    	
    	
    	if (mOggFile == null){
    		// TODO Put in error message
    		mCurrentState = CreateState.idle_record;
        	activateState(false);
    		btnAction.setEnabled(true);
    		return;
    	}
    	
    	
    	
    	mMediaPlayer.reset();
    	
    	try {
        	FileInputStream fis = new FileInputStream(mOggFile);
			mMediaPlayer.setDataSource(fis.getFD());
	        mMediaPlayer.prepareAsync();
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

    }
    
   private void startPlayback(){
	   mMediaPlayer.start();
   	
   		isPlayingBack = true;
   		// Start lengthy operation in a background thread
   		new Thread(new Runnable() {
           public void run() {
               while (isPlayingBack) {
               	if (!mDragging){
               		playbackPosition = mMediaPlayer.getCurrentPosition();
               	    // Update the progress bar
                   mHandler.post(new Runnable() {
                       public void run() {
                    	   
                    	   if (!mDragging && isPlayingBack)
                           mProgressBar.setProgress(playbackPosition);
                    	   
                       }
                   });
                   
                   try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();} 
               	}
               }
           }
   		}).start();
   }
    
   private void pausePlayback(){
	   	if (mMediaPlayer.isPlaying())
				mMediaPlayer.pause();
	   	
	   	isPlayingBack = false;
   }
   
    
    
    MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
        	Log.i(TAG,"MEDIA PLAYER PREPARED ");
        	mProgressBar.setMax(mMediaPlayer.getDuration()); 
        	mCurrentState = CreateState.idle_playback;
        	activateState(false);
        	
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
         	mCurrentState = CreateState.idle_playback;
    	   	activateState(false);
    	   	mProgressBar.setProgress(0);
    	   
        }
    };
    
    private int playbackPosition;
	private long mLastSeekEventTime;
    
	private boolean mFromTouch = false;
	private boolean mDragging = false;
    
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			mLastSeekEventTime = 0;
			mFromTouch = true;
			mDragging = true;
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			
			if (!fromuser) {
				return;
			}
			
			long now = SystemClock.elapsedRealtime();
			if (now - mLastSeekEventTime > 250) {
				mLastSeekEventTime = now;
				mMediaPlayer.seekTo(progress);
			}
		}

		public void onStopTrackingTouch(SeekBar bar) {
			mFromTouch = false;
			mDragging = false;
		}
	};
    
    private void startUpload(){
    	Time time = new Time();
    	time.setToNow();
       
    	// Calendar header
    	String dayOfWeek = DateUtils.getDayOfWeekString(time.weekDay + 1,DateUtils.LENGTH_LONG).toLowerCase();
    	
    	String title = "";
    	if (!CloudUtils.stringNullEmptyCheck(mWhatText.getText().toString()) && !CloudUtils.stringNullEmptyCheck(mWhereText.getText().toString()))
    		title = mWhatText.getText().toString() + " at " + mWhereText.getText().toString() + " on " + dayOfWeek;
    	else if (!CloudUtils.stringNullEmptyCheck(mWhatText.getText().toString()))
    		title = mWhatText.getText().toString() + " on " + dayOfWeek;
    	else if (!CloudUtils.stringNullEmptyCheck(mWhereText.getText().toString()))
    		title = mWhereText.getText().toString() + " on " + dayOfWeek;
    	else
    		title = "recording on " + dayOfWeek;
    	
    	
    	
    	mActivity.showDialog(Dialogs.DIALOG_RECORD_UPLOADING);
    	mActivity.getProgressDialog().setMax((int) mOggFile.length());
    	
    	/*mActivity.getProgressDialog().setMax((int) mWavFile.length());
    	
    	mActivity.getProgressDialog().setOnCancelListener(new OnCancelListener(){
    		@Override
			public void onCancel(DialogInterface dialog) {
				if (uploadThread.isAlive())
					uploadThread.interrupt();			
			}
    		
    	});*/
    	
    	final List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();
    		
    	 params.add(new BasicNameValuePair("track[title]", title));
    	 //params.add(new BasicNameValuePair("track[tag_list]", "soundcloud:android-record"));
    	 
    	 if (mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private){
    		 params.add(new BasicNameValuePair("track[sharing]", "private"));
    	 } else {
    		 params.add(new BasicNameValuePair("track[sharing]", "public"));
    	 }
    	 
    	 
    	 final ProgressFileBody trackBody = new ProgressFileBody(mOggFile);
    	 final ProgressFileBody artBody = mArtworkUri == null ? null : new ProgressFileBody(new File(mArtworkUri));
    	   
    	   uploadThread = new Thread(new Runnable()
    	   {
    	       public void run()
    	               {
    	                       try
    	                       {
    	                    	   mActivity.mCloudComm.getApi().upload(trackBody, params);
    	                    	   
    	                       } catch (Exception e)
    	                       {
    	                               e.printStackTrace();
    	                       }
    	               }
    	       });
    	   
    	   uploadThread.start();
    	   
    	   
    	// Start lengthy operation in a background thread
      		new Thread(new Runnable() {
              public void run() {
            	  
            	  while(uploadThread.isAlive()){
	           		   
	           		  //show progress using handler for ui thread
                      mHandler.post(new Runnable() {
                          public void run() {
                        	  //mActivity.getProgressDialog().setProgress(artBody != null ? (int) trackBody.getBytesTransferred(): (int) trackBody.getBytesTransferred() + (int)trackBody.getBytesTransferred());
                        	  mActivity.getProgressDialog().setProgress((int)trackBody.getBytesTransferred());
                          }
                      });
                      
                      try {Thread.sleep(200);} catch (InterruptedException e) {e.printStackTrace();}
	                  	
	           	   }
            	  
            	  //show progress using handler for ui thread
                  mHandler.post(new Runnable() {
                      public void run() {
                    	  mActivity.removeDialog(Dialogs.DIALOG_RECORD_UPLOADING);
                    	  clearRecording();
                    	  
                   	   	  ((Main) mActivity).gotoUserTab(UserBrowser.UserTabs.tracks);
                   	   	  //((Main) mActivity).showDialog(Dialogs.DIALOG_RECORD_UPLOADING_SUCCESS);
                      }
                  });
                 
              }
      		}).start();

    	}
    

   
    
    
   
    public void onOggEncodeComlete(Boolean result){
    	mActivity.dismissDialog(Dialogs.DIALOG_PROCESSING);
    	
    	if (result){
    		
    		if (mRecordFile.exists())
    			mRecordFile.delete();
    		
    		preparePlayback();
    	} else {
    		mActivity.showDialog(Dialogs.DIALOG_RECORD_PROCESSING_FAILED);
    	}
    }
    
   

   
    
    
    private class EncodeVorbisTask extends VorbisEncoderTask {
		public LazyActivity activity;
		public ScCreate createRef;
		public int waveFileSize;

		@Override
		protected void onPreExecute() {
			if (activity != null){
				activity.showDialog(Dialogs.DIALOG_PROCESSING);
				activity.getProgressDialog().setIndeterminate(true);
				activity.getProgressDialog().setMax(100);
				activity.getProgressDialog().setProgress(0);
				activity.getProgressDialog().setTitle(activity.getResources().getString(R.string.record_preparing_wav_title));
			}
		}


		@Override
		protected void onProgressUpdate(Integer... progress) {
			Log.i(TAG,"On progress update " + activity.getResources().getString(R.string.record_encoding_wav_title));
			activity.getProgressDialog().setTitle(activity.getResources().getString(R.string.record_encoding_wav_title));
			activity.getProgressDialog().setIndeterminate(false);
			activity.getProgressDialog().setMax(progress[1]);
			activity.getProgressDialog().setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (createRef != null) createRef.onOggEncodeComlete(result);
		}
	}
    
/*
    
     public void onWavEncodeComplete(Boolean result){
    	if (result){
    		//if (mRecordFile.exists())
        		//mRecordFile.delete();
        	
    		EncodeVorbisTask processTask = new EncodeVorbisTask();
    		processTask.activity = mActivity;
    		processTask.createRef = this;
    		processTask.waveFileSize = (int) mWavFile.length();
    		processTask.execute(mRecordFile.getAbsolutePath(), mOggFile.getAbsolutePath());
    	} else {
    		mActivity.dismissDialog(Dialogs.DIALOG_PROCESSING);
    		mActivity.showDialog(Dialogs.DIALOG_RECORD_PROCESSING_FAILED);
    	}
    }
    
    
    private class EncodePCMTask extends WavEncoderTask {
		public LazyActivity activity;
		public ScCreate createRef;

		@Override
		protected void onPreExecute() {
			if (activity != null){
				activity.showDialog(Dialogs.DIALOG_RECORD_PROCESSING);
				activity.getProgressDialog().setIndeterminate(true);
				activity.getProgressDialog().setTitle(activity.getResources().getString(R.string.record_reading_pcm_message));
			}
		}


		@Override
		protected void onProgressUpdate(Integer... progress) {
			Log.i(TAG,"on progress update " + activity);
			if (activity != null){
				if (activity.getProgressDialog() != null){
					
					//progress[0] represents what stage the task is in
					switch (progress[0]){
						case stages.reading:
							activity.getProgressDialog().setIndeterminate(false);
							activity.getProgressDialog().setMax(progress[2]);
							activity.getProgressDialog().setProgress(progress[1]);
							break;
							
						case stages.writing:
							activity.getProgressDialog().setIndeterminate(false);
							activity.getProgressDialog().setMessage(activity.getResources().getString(R.string.record_writing_wav_message));
							break;
					}
				}
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (createRef != null) createRef.onWavEncodeComplete(result);
		}
	}*/
	
}
