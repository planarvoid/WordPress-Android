package com.soundcloud.android.view;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.task.PCMPrepareTask;
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
    private RelativeLayout mProgressFrame;
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
    private File mWavFile;
    private String mArtworkUri;
    
    private LazyActivity mActivity;
    
    private CreateState mLastState;
    private CreateState mCurrentState;
    private CreateState mStoredState;
    
    private Chronometer mPlayChrono;
    private Chronometer mRecChrono;
    private TextView mChronoSep;
    
	public enum CreateState { idle_record, record, idle_playback, playback, idle_upload, upload }
    
	private Boolean isPlayingBack = false;
	private Handler mHandler = new Handler();
	
	private Thread uploadThread;
	
	private int mPlaybackLength;
	
	private AudioTrack playbackTrack;
	private PCMPrepareTask prepareTask;
	

	

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
		
		mRecChrono = (Chronometer) findViewById(R.id.recChronometer);
		mRecChrono.setVisibility(View.GONE);
		
		mPlayChrono = (Chronometer) findViewById(R.id.playChronometer);
		mPlayChrono.setVisibility(View.GONE);
		
		mChronoSep = (TextView) findViewById(R.id.txtChronoSeparator);
		mPlayChrono.setVisibility(View.GONE);
		
		mProgressBar.setOnSeekBarChangeListener(mSeekListener);
		
		mProgressFrame = (RelativeLayout) findViewById(R.id.progress_frame);
		mProgressFrame.setVisibility(View.GONE);
		
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
		
		mWhatText.setOnFocusChangeListener(txtFocusListener);
		mWhereText.setOnFocusChangeListener(txtFocusListener);
		
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
    	
		restoreRecordings();
	}
    
    public int getCurrentState(){
    	Log.i(TAG,"Get Current State " + mCurrentState);
    	switch (mCurrentState){
    	case idle_record:
    		return 0;
    	case record:
    		return 1;
    	case idle_playback:
    		return 2;
    	case playback:
    		return 3;
    	case idle_upload:
    		return 4;
    	case upload:
    		return 5;
    	}
		return 0;
    }
    
    public void setCurrentState(int state){
    	Log.i(TAG,"Activate Current State " + state);
    	switch (state){
    	case 0:
    		mCurrentState = CreateState.idle_record;
    			break;
    	case 1:
    		mCurrentState = CreateState.record;
    			break;
    	case 2:
    		mCurrentState = CreateState.idle_playback;
    			break;
    	case 3:
    		mCurrentState = CreateState.playback;
    			break;
    	case 4:
    		mCurrentState = CreateState.idle_upload;
    			break;
    	case 5:
    		mCurrentState = CreateState.upload;
    			break;
    	
    	}
		activateState();
    }
    
    
    @Override
	public void onStart() {
    	super.onStart();
    	
    	if (!mWavFile.exists()){
    		  mCurrentState = CreateState.idle_record;
    	} else {
    		//preparePlayback();
    	}
    	
    	activateState();
    	
    }
    
    /*public void setCurrentState(int state){
    	Log.i(TAG,"Activate Current State " + state);
    	switch (state){
    	case 0:
    		mStoredState = mCurrentState = CreateState.idle_record;
    			break;
    	case 1:
    		mStoredState = mCurrentState = CreateState.idle_record;
    			break;
    	case 2:
    		mStoredState = mCurrentState = CreateState.idle_record;
    			break;
    	case 3:
    		mStoredState = mCurrentState = CreateState.idle_record;
    			break;
    	case 4:
    		mStoredState = mCurrentState = CreateState.idle_record;
    			break;
    	case 5:
    		mStoredState = mCurrentState = CreateState.idle_record;
    			break;
    	
    	}
		
    }
    
    
    @Override
	public void onStart() {
    	super.onStart();
    	
    	Log.i(TAG,"On Start " + mStoredState);
    	
    	if (mStoredState != null){
    		mCurrentState = mStoredState;
    	} if (!mWavFile.exists()){
    		  mCurrentState = CreateState.idle_record;
    	} else {
    		//preparePlayback();
    	}
    	
    	activateState();
    	
    }*/
    
    
    @Override
    public void onStop() {
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
    
    public void hideProgressFrame(){
    	mProgressFrame.setVisibility(View.GONE);
    }
    
    public void showProgressFrame(){
    	mProgressFrame.setVisibility(View.VISIBLE);
    }
    
    
    private void restoreRecordings(){
    	mRecordFile = new File(mActivity.getCacheDir() + "/rec.pcm" );
    	mWavFile = new File(mActivity.getCacheDir() + "/rec.wav");
	  	
	  	//reset the pcm and wav files just in case they are taking up space
	  	clearSourceFiles();
	  	
	  	if (mWavFile.exists()){
	  		
	  		mCurrentState = CreateState.idle_playback;
	  		activateState();
	  	} 
	  	
		
    }
    
    private void clearRecording(){
    	clearSourceFiles();
    	if (mWavFile.exists()) mWavFile.delete();
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
				clearPlaybackTrack();
				
				mWhereText.setText("");
				mWhatText.setText("");
				clearArtwork();
				
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_states));
				mFileLayout.setVisibility(View.GONE);
				
				mRecChrono.setVisibility(View.GONE);
				mPlayChrono.setVisibility(View.GONE);
				mChronoSep.setVisibility(View.GONE);
				
				mProgressBar.setVisibility(View.GONE);
				mPowerGauge.setVisibility(View.GONE);
				txtInstructions.setVisibility(View.VISIBLE);
				mRecorder.deactivate();
				
				break;
				
			case record:
				flipLeft();
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_stop_states));
				txtInstructions.setVisibility(View.GONE);
				
				mRecChrono.setBase(SystemClock.elapsedRealtime());
				mRecChrono.setVisibility(View.VISIBLE);
				
				mPlayChrono.setVisibility(View.GONE);
				mChronoSep.setVisibility(View.GONE);
				
				mPowerGauge.setVisibility(View.VISIBLE);
				
				if (takeAction)
					startRecording();
				
				break;
				
			case idle_playback:
				flipLeft();
				
				mRecChrono.setVisibility(View.VISIBLE);
				mPlayChrono.setVisibility(View.GONE);
				mChronoSep.setVisibility(View.GONE);
				
				if (takeAction) {
					if (mLastState == CreateState.record) stopRecording();
					if (mLastState == CreateState.playback) pausePlayback();
				}
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_play_states));
				txtInstructions.setVisibility(View.GONE);
				mFileLayout.setVisibility(View.VISIBLE);
				mPowerGauge.setVisibility(View.GONE);
				mProgressBar.setVisibility(View.VISIBLE);
				break;
				
			case playback:
				flipLeft();
				
				mRecChrono.setVisibility(View.VISIBLE);
				
				
				btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_stop_states));
				//btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.btn_rec_pause_states));
				if (takeAction) 
					startPlayback();
				break;
				
			case idle_upload:
				flipRight();
				break;	
			
			case upload:
				clearPlaybackTrack();
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
		mRecChrono.start();
    }
    
    private void stopRecording(){
    	
    	//disable actions during processing and playback preparation
    	btnAction.setEnabled(false);
    	
		mRecorder.stopRecording();
		mRecChrono.stop();
		mRecorder.deactivate();
	
		
		
		
    }
    
   private void clearPlaybackTrack(){
	   if (prepareTask != null){
   		if (!CloudUtils.isTaskFinished(prepareTask))
   			prepareTask.cancel(true);
   		}
	   
	   if (playbackTrack != null){
		   playbackTrack.release();
		   playbackTrack = null;
	   }
	   
   }
    
   private void startPlayback(){
	   Log.i("CHRONO","Length : " + mRecordFile.length());
	   mPlaybackLength = (int) Math.floor(mRecordFile.length()/(44100*2));
		Log.i("CHRONO","Length : " + mPlaybackLength);
	   
	   Log.i(TAG,"Start Playback " + playbackTrack);
	   if (playbackTrack != null){
		   clearPlaybackTrack();
	   } 
	   
	   int minSize =AudioTrack.getMinBufferSize( 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT );        
		playbackTrack = new AudioTrack( AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
		playbackTrack.play();
		
	   	prepareTask = new PCMPrepareTask();
	   	prepareTask.pcmFile = mRecordFile;
	   	prepareTask.minSize = minSize;
	   	prepareTask.playbackTrack = playbackTrack;
	   	prepareTask.execute();
	   	
	   	mProgressBar.setMax((int) (mRecordFile.length()/2));
	   
	   isPlayingBack = true;
	   startProgressTracker();
	   
	  
	   mPlayChrono.setBase(SystemClock.elapsedRealtime());
	   mPlayChrono.start();
	   mPlayChrono.setVisibility(View.VISIBLE);
	   mChronoSep.setVisibility(View.VISIBLE);
   }
   
   private void pausePlayback(){
	   	playbackTrack.pause();
	   	isPlayingBack = false;
	    
	   	mPlayChrono.stop();
		
	   	mProgressBar.setProgress(0);
  }
   
   private void startProgressTracker(){
	// Start lengthy operation in a background thread
  		new Thread(new Runnable() {
          public void run() {
              while (isPlayingBack) {
              	if (!mDragging){
              		
              		int currentPosition = (int) Math.floor(playbackTrack.getPlaybackHeadPosition()/(44100));
              	    
            		// Update the progress bar
              		mHandler.post(new Runnable() {
                      public void run() {
                   	   if (!mDragging && isPlayingBack){
                          mProgressBar.setProgress(playbackTrack.getPlaybackHeadPosition());
                   	   }
                      }
                  });
                  
              		if (playbackTrack.getPlaybackHeadPosition() == mRecordFile.length()/2){
              			mHandler.post(new Runnable() {
                            public void run() {
                         	   playbackComplete();
                            }
                        });
              		}
              		
                  try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();} 
              	}
              }
          }
  		}).start();
   }
   
   private void playbackComplete(){
	   Log.i(TAG,"Playback complete");
	   
	   isPlayingBack = false;
	   mProgressBar.setProgress(0);
	   
	   mCurrentState = CreateState.idle_playback;
	   activateState();
   }
    
    
    private int playbackPosition;
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
	
	
	private void startUpload(){
		
		Time time = new Time();
    	time.setToNow();
       
    	// Calendar header
    	String dayOfWeek = DateUtils.getDayOfWeekString(time.weekDay + 1,DateUtils.LENGTH_LONG).toLowerCase();
    	
    	String title = "";
    	if (!CloudUtils.stringNullEmptyCheck(mWhatText.getText().toString()) && !CloudUtils.stringNullEmptyCheck(mWhereText.getText().toString()))
    		title = mWhatText.getText().toString() + " at " + mWhereText.getText().toString();
    	else if (!CloudUtils.stringNullEmptyCheck(mWhatText.getText().toString()))
    		title = mWhatText.getText().toString();
    	else if (!CloudUtils.stringNullEmptyCheck(mWhereText.getText().toString()))
    		title = mWhereText.getText().toString();
    	else
    		title = "recording on " + dayOfWeek;
    	
    	 
    	HashMap<String,String> trackdata = new HashMap<String,String>();
    	
    	 
    	 
    	 if (mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private){
    		 trackdata.put("track[sharing]", "private");
    	 } else {
    		 trackdata.put("track[sharing]", "public");
    	 }
		
		trackdata.put("pcm_path", mRecordFile.getAbsolutePath());
		trackdata.put("track[title]", title);
		
		try {
			((LazyActivity) mActivity).getUploadService().uploadTrack(trackdata);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mCurrentState = CreateState.idle_record;
		activateState();
	}
	
	
    private OnFocusChangeListener txtFocusListener = new View.OnFocusChangeListener() {
		public void onFocusChange(View v, boolean hasFocus) {
			InputMethodManager mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (hasFocus == false){
				
				if (!CloudUtils.stringNullEmptyCheck(((TextView) v).getText().toString()))
					((TextView) v).setText(CloudUtils.toTitleCase(((TextView) v).getText().toString()));
					
				if (mgr != null) 
					mgr.hideSoftInputFromWindow(mWhatText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
		}
	};
	
	
	
	
	
}
