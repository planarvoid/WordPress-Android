
package com.soundcloud.android.view;

import android.widget.*;
import android.widget.Button;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.task.PCMPlaybackTask;
import com.soundcloud.android.task.PCMPlaybackTask.PlaybackListener;
import com.soundcloud.utils.AnimUtils;
import com.soundcloud.utils.record.PowerGauge;
import com.soundcloud.utils.record.RemainingTimeCalculator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ScCreate extends ScTabView implements PlaybackListener {

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "ScCreate";

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    private ViewFlipper mViewFlipper, mSharingFlipper;

    private TextView txtInstructions;

    private TextView txtRecordStatus;

    private LinearLayout mFileLayout;

    private FrameLayout mGaugeHolder;

    private RelativeLayout mProgressFrame;

    private PowerGauge mPowerGauge;

    private SeekBar mProgressBar;

    private RadioGroup mRdoPrivacy;

    private RadioButton mRdoPrivate;

    private RadioButton mRdoPublic;

    private EditText mWhereText;

    private EditText mWhatText;

    private TextView mArtworkInstructions;

    private ImageView mArtwork;

    private ImageButton btnAction;

    private Button btnReset;

    private Button btnCancel;

    private Button btnSave;

    private Button btnUpload;

    private Button btnCancelUpload;

    private File mRecordFile;

    private String mArtworkUri;

    private Bitmap mArtworkBitmap;

    private int mArtworkInSampleSize;

    private LazyActivity mActivity;

    private CreateState mLastState;

    private CreateState mCurrentState;

    private CreateState mStoredState;

    private TextView mChrono;
    private ConnectionList mConnectionList;

    public enum CreateState {
        idle_record, record, idle_playback, playback, idle_upload, upload
    }

    private Boolean isRecording = false;

    private String mRecordErrorMessage = "";

    private Boolean isPlayingBack = false;

    private Thread uploadThread;

    private int mPlaybackLength;

    // private PCMRecordTask mRecordTask;
    private PCMPlaybackTask mPlaybackTask;

    private AudioTrack playbackTrack;

    private String mDurationFormatLong;

    private String mDurationFormatShort;

    private String mCurrentDurationString;

    private String progressLengthString;

    private String progressCounterString;

    public static int REC_SAMPLE_RATE = 44100;

    public static int REC_CHANNELS = 2;

    public static int REC_BITS_PER_SAMPLE = 16;

    // public static int REC_MAX_FILE_SIZE = 52920000; //5 mins at
    // 44100x16bitx2channels
    // public static int REC_MAX_FILE_SIZE = 105840000; //10 mins at
    // 44100x16bitx2channels
    public static int REC_MAX_FILE_SIZE = 158760000; // 15 mins at

    // 44100x16bitx2channels

    // public static int REC_MAX_FILE_SIZE = 211680000; //20 mins at
    // 44100x16bitx2channels

    static final String STATE_FILE_NAME = "soundrecorder.state";

    static final String RECORDER_STATE_KEY = "recorder_state";

    static final String SAMPLE_INTERRUPTED_KEY = "sample_interrupted";

    static final String MAX_FILE_SIZE_KEY = "max_file_size";

    static final String AUDIO_3GPP = "audio/3gpp";

    static final String AUDIO_AMR = "audio/amr";

    static final String AUDIO_ANY = "audio/*";

    static final String ANY_ANY = "*/*";

    static final int BITRATE_AMR = 5900; // bits/sec

    static final int BITRATE_3GPP = 5900;

    WakeLock mWakeLock;

    String mRequestedType = AUDIO_ANY;

    boolean mSampleInterrupted = false;

    String mErrorUiMessage = null; // Some error messages are displayed in the

    // UI,

    // not a dialog. This happens when a recording
    // is interrupted for some reason.

    long mMaxFileSize = -1; // can be specified in the intent

    RemainingTimeCalculator mRemainingTimeCalculator;

    String mTimerFormat;

    final Handler mHandler = new Handler();

    private BroadcastReceiver mSDCardMountEventReceiver = null;

    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
     * Called when the activity is starting. This is where most initialisation
     * should go: calling setContentView(int) to inflate the activity's UI, etc.
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of the
     * activity lifecycle executing. Derived classes must call through to the
     * super class's implementation of this method. If they do not, an exception
     * will be thrown.
     * 
     * @param icicle If the activity is being re-initialised after previously
     *            being shut down then this Bundle contains the data it most
     *            recently supplied in onSaveInstanceState(Bundle). Note:
     *            Otherwise it is null.
     */

    public ScCreate(LazyActivity activity) {
        super(activity);

        mActivity = activity;
        mCurrentState = CreateState.idle_record;

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sc_create, this);

        mDurationFormatLong = mActivity.getString(R.string.durationformatlong);
        mDurationFormatShort = mActivity.getString(R.string.durationformatshort);

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * REC_CHANNELS * REC_BITS_PER_SAMPLE);

        PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "SoundRecorder");


        initResourceRefs();

        updateUi(false);
        restoreRecordings();
    }

    /*
     * Whenever the UI is re-created (due f.ex. to orientation change) we have
     * to reinitialize references to the views.
     */
    private void initResourceRefs() {

        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);

        mFileLayout = (LinearLayout) findViewById(R.id.file_layout);
        mGaugeHolder = (FrameLayout) findViewById(R.id.gauge_holder);
        txtInstructions = (TextView) findViewById(R.id.txt_instructions);
        mProgressBar = (SeekBar) findViewById(R.id.progress_bar);

        txtRecordStatus = (TextView) findViewById(R.id.txt_record_status);

        mChrono = (TextView) findViewById(R.id.chronometer);
        mChrono.setVisibility(View.GONE);

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
                updateUi();
            }
        });

        btnSave = (Button) findViewById(R.id.btn_save);
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.idle_upload;
                updateUi();
            }
        });

        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.idle_playback;
                updateUi();
            }
        });

        btnUpload = (Button) findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.upload;
                updateUi();
            }
        });

        mArtwork = (ImageView) findViewById(R.id.artwork);
        mArtworkInstructions = (TextView) findViewById(R.id.txt_artwork_instructions);
        mWhatText = (EditText) findViewById(R.id.what);
        mWhereText = (EditText) findViewById(R.id.where);

        mWhatText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (mWhatText.length() == 1
                        && !mWhatText.getText().toString().toUpperCase().contentEquals(
                                mWhatText.getText().toString())) {
                    mWhatText.setTextKeepState(mWhatText.getText().toString().toUpperCase());
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        mWhereText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (mWhatText.length() == 1
                        && !mWhereText.getText().toString().toUpperCase().contentEquals(
                                mWhatText.getText().toString()))
                    mWhatText.setTextKeepState(mWhatText.getText().toString().toUpperCase());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // mWhatText.setOnFocusChangeListener(txtFocusListener);
        // mWhereText.setOnFocusChangeListener(txtFocusListener);

        mSharingFlipper = (ViewFlipper) findViewById(R.id.vfSharing);
        mRdoPrivacy = (RadioGroup) findViewById(R.id.rdo_privacy);
        mRdoPublic = (RadioButton) findViewById(R.id.rdo_public);
        mRdoPrivate = (RadioButton) findViewById(R.id.rdo_private);

        mRdoPrivacy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rdo_public:   mSharingFlipper.setDisplayedChild(0); break;
                    case R.id.rdo_private:  mSharingFlipper.setDisplayedChild(1); break;

                }
            }
        });

        mArtwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                mActivity
                        .startActivityForResult(intent, CloudUtils.RequestCodes.GALLERY_IMAGE_PICK);
            }
        });

        mArtwork.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clearArtwork();
                return true;
            }
        });

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mPowerGauge = new PowerGauge(mActivity);
        mGaugeHolder.addView(mPowerGauge);
        mTimerFormat = getResources().getString(R.string.timer_format);
    }

    private void initConnections() {
        if (mConnectionList == null) {
            mConnectionList = (ConnectionList) findViewById(R.id.connectionList);
            mConnectionList.setAdapter(new ConnectionList.Adapter()
                .load(mActivity.getSoundCloudApplication()));
        }

    }

    /*** Public ***/

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private)
            outState.putString("createCurrentCreateStateIndex", Integer.toString(getCurrentState()));
            outState.putString("createWhatValue", mWhatText.getText().toString());
            outState.putString("createWhereValue", mWhereText.getText().toString());
            outState.putInt("createPrivacyValue", mRdoPrivacy.getCheckedRadioButtonId());
        if (!TextUtils.isEmpty(mArtworkUri))
            outState.putString("createArtworkPath", mArtworkUri);
        
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private) {

        }
        String currentCreateStateIndex = savedInstanceState
                .getString("createCurrentCreateStateIndex") == "null"
                || savedInstanceState.getString("createCurrentCreateStateIndex") == null ? "0"
                : savedInstanceState.getString("createCurrentCreateStateIndex");
        if (!TextUtils.isEmpty(savedInstanceState
                .getString("createCurrentCreateStateIndex")))
            setCurrentState(Integer.parseInt(currentCreateStateIndex));

        mWhatText.setText(savedInstanceState.getString("createWhatValue"));
        mWhereText.setText(savedInstanceState.getString("createWhatValue"));

        if (savedInstanceState.getInt("createPrivacyValue") == R.id.rdo_private)
            mRdoPrivate.setChecked(true);
        else
            mRdoPublic.setChecked(true);

        if (!TextUtils.isEmpty(savedInstanceState.getString("createArtworkPath")))
            setPickedImage(savedInstanceState.getString("createArtworkPath"));

        super.onRestoreInstanceState(savedInstanceState);
    }

    public int getCurrentState() {
        if (mCurrentState == null)
            return 0;

        switch (mCurrentState) {
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

    public void setCurrentState(int state) {
        switch (state) {
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

        updateUi(false);
    }

    public void onRecordingError() {
        mSampleInterrupted = true;
        mRecordErrorMessage = getResources().getString(R.string.error_recording_message);
        mCurrentState = CreateState.idle_record;
        updateUi(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            if (mActivity.getCreateService() != null && mActivity.getCreateService().isUploading()) {
                mCurrentState = CreateState.upload;
            } else if (mCurrentState == CreateState.upload) {
                mCurrentState = CreateState.idle_record;
            } else if (mCurrentState == null) {

                if (!mRecordFile.exists()) {
                    mCurrentState = CreateState.idle_record;
                } else {
                    mCurrentState = CreateState.idle_playback;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            mCurrentState = CreateState.idle_record;
        }
        updateUi(false);
    }

    @Override
    public void onStop() {

    }

    @Override public void onAuthenticated() {
        initConnections();
    }

    public void unlock(Boolean finished) {
        // not currently uploading anything, so allow recording
        if (mCurrentState == CreateState.upload) {
            if (finished)
                mCurrentState = CreateState.idle_record;
            else
                mCurrentState = CreateState.idle_playback;

            updateUi(false);
        }
    }

    public PCMPlaybackTask getPlaybackTask() {
        return mPlaybackTask;
    }

    public void setPlaybackTask(PCMPlaybackTask playbackTask) {
        mPlaybackTask = playbackTask;
        if (mPlaybackTask != null)
            mPlaybackTask.setPlaybackListener(this);
    }

    public void setPickedImage(String imageUri) {
        Options opt;
        try {

            opt = CloudUtils.determineResizeOptions(imageUri, mArtwork.getWidth(), mArtwork
                    .getHeight(), true);
            mArtworkUri = imageUri;
            mArtworkInSampleSize = opt.inSampleSize;

            if (mArtworkBitmap != null)
                CloudUtils.clearBitmap(mArtworkBitmap);

            mArtworkBitmap = BitmapFactory.decodeFile(mArtworkUri);
            mArtwork.setImageBitmap(mArtworkBitmap);

            mArtworkInstructions.setText(mActivity.getResources().getString(
                    R.string.record_artwork_instructions_added));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void clearArtwork() {
        mArtworkUri = null;
        mArtwork.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.artwork_player));
        mArtworkInstructions.setText(mActivity.getResources().getString(
                R.string.record_artwork_instructions));

        if (mArtworkBitmap != null)
            CloudUtils.clearBitmap(mArtworkBitmap);
    }

    public void hideProgressFrame() {
        mProgressFrame.setVisibility(View.GONE);
    }

    public void showProgressFrame() {
        mProgressFrame.setVisibility(View.VISIBLE);
    }

    private void restoreRecordings() {

        mRecordFile = new File(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/rec.mp4");

        // if (mRecordFile.exists()){
        // mCurrentState = CreateState.idle_playback;
        // updateUi();
        // }

    }

    /*** State Handling ***/
    private void onAction() {
        switch (mCurrentState) {
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

        updateUi();
    }

    private void updateUi() {
        updateUi(true);
    }

    private void updateUi(Boolean takeAction) {

        Log.i(TAG, "Update Soundcloud Create state: " + mCurrentState + " | take action: "
                + takeAction);
        switch (mCurrentState) {
            case idle_record:
                goToView(0);
                if (takeAction)
                    clearPlaybackTrack();

                btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(
                        R.drawable.btn_rec_states));
                mFileLayout.setVisibility(View.GONE);

                if (TextUtils.isEmpty(mRecordErrorMessage))
                    txtRecordStatus.setText(mActivity.getResources().getString(
                            R.string.cloud_recorder_experimental));
                else
                    txtRecordStatus.setText(mRecordErrorMessage);

                txtRecordStatus.setVisibility(View.VISIBLE);

                mChrono.setVisibility(View.GONE);

                mProgressBar.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.VISIBLE);

                break;

            case record:
                goToView(0);
                if (mViewFlipper.getDisplayedChild() != 0)
                    mViewFlipper.setDisplayedChild(0);

                btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(
                        R.drawable.btn_rec_stop_states));
                txtInstructions.setVisibility(View.GONE);
                txtRecordStatus.setText("");
                txtRecordStatus.setVisibility(View.VISIBLE);
                mChrono.setText("0.00");
                mChrono.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
                mFileLayout.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.VISIBLE);

                if (takeAction)
                    startRecording();

                break;

            case idle_playback:
                goToView(0);

                calculateTotalProgress();
                mChrono.setText(mCurrentDurationString);

                if (takeAction) {
                    if (mLastState == CreateState.record)
                        stopRecording();
                    if (mLastState == CreateState.playback)
                        pausePlayback();
                }

                btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(
                        R.drawable.btn_rec_play_states));
                txtInstructions.setVisibility(View.GONE);
                mFileLayout.setVisibility(View.VISIBLE);
                mPowerGauge.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mChrono.setVisibility(View.VISIBLE);

                // if (TextUtils.isEmpty(mRecordErrorMessage)){
                // txtRecordStatus.setText(mActivityReference.getResources().getString(R.string.cloud_recorder_experimental));
                // txtRecordStatus.setVisibility(View.VISIBLE);
                // } else
                txtRecordStatus.setVisibility(View.GONE);

                break;

            case playback:

                goToView(0);
                calculateTotalProgress();
                txtRecordStatus.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.GONE);
                mFileLayout.setVisibility(View.VISIBLE);
                mPowerGauge.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mChrono.setVisibility(View.VISIBLE);
                btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(
                        R.drawable.btn_rec_stop_states));
                // btnAction.setBackgroundDrawable(mActivityReference.getResources().getDrawable(R.drawable.btn_rec_pause_states));
                if (takeAction)
                    startPlayback();
                break;

            case idle_upload:
                goToView(1);
                break;

            case upload:
                goToView(2);
                clearPlaybackTrack();
                flipRight();
                if (takeAction)
                    startUpload();
                break;

        }

        mLastState = mCurrentState;
        btnAction.setEnabled(true);
    }

    private void calculateTotalProgress() {
        mCurrentDurationString = getMinsSecsString(mRecordFile, REC_SAMPLE_RATE, REC_CHANNELS,
                REC_BITS_PER_SAMPLE);
    }

    /*** View Flipping ***/

    private void goToView(int child) {
        switch (mViewFlipper.getDisplayedChild()) {
            case 0:
                switch (child) {
                    case 1:
                        flipRight();
                        break;
                    case 2:
                        flipDirect(2);
                        break;
                }
                break;
            case 1:
                switch (child) {
                    case 0:
                        flipLeft();
                        break;
                    case 2:
                        flipRight();
                        break;
                }
                break;
            case 2:
                switch (child) {
                    case 0:
                        flipDirect(0);
                        break;
                    case 1:
                        flipLeft();
                        break;
                }
                break;
        }
    }

    private void flipDirect(int index) {
        mViewFlipper.setInAnimation(null);
        mViewFlipper.setOutAnimation(null);
        mViewFlipper.setDisplayedChild(index);
    }

    private void flipRight() {
        if (mViewFlipper.getDisplayedChild() == 2)
            return;

        mViewFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
        mViewFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
        mViewFlipper.showNext();
    }

    private void flipLeft() {
        if (mViewFlipper.getDisplayedChild() == 0)
            return;

        mViewFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
        mViewFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
        mViewFlipper.showPrevious();
    }

    /*** Record Handling ***/

    private void startRecording() {

        mActivity.forcePause();

        mRecordErrorMessage = "";
        mSampleInterrupted = false;

        mRemainingTimeCalculator.reset();
        mPowerGauge.clear();

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_storage_is_full);
        }

        if (mSampleInterrupted) {
            mCurrentState = CreateState.idle_record;
            updateUi();
            return;
        }

        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * REC_CHANNELS * REC_BITS_PER_SAMPLE);
        if (REC_MAX_FILE_SIZE != -1) {
            mRemainingTimeCalculator.setFileSizeLimit(mRecordFile, REC_MAX_FILE_SIZE);
        }

        mActivity.setRequestedOrientation(mActivity.getResources().getConfiguration().orientation);
        try {
            (mActivity).getCreateService().startRecording(mRecordFile.getAbsolutePath());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mActivity.getSoundCloudApplication().setRecordListener(recListener);

    }

    /*
     * Called when we're in recording state. Find out how much longer we can go
     * on recording. If it's under 5 minutes, we display a count-down in the UI.
     * If we've run out of time, stop the recording.
     */
    private void updateTimeRemaining() {

        long t = mRemainingTimeCalculator.timeRemaining() + 2; // adding 2
        // seconds to
        // make up for
        // lag
        if (t <= 1) {
            mSampleInterrupted = true;

            int limit = mRemainingTimeCalculator.currentLowerLimit();
            switch (limit) {
                case RemainingTimeCalculator.DISK_SPACE_LIMIT:
                    mRecordErrorMessage = getResources().getString(R.string.record_storage_is_full);
                    break;
                case RemainingTimeCalculator.FILE_SIZE_LIMIT:
                    mRecordErrorMessage = getResources().getString(
                            R.string.record_max_length_reached);
                    break;
                default:
                    mRecordErrorMessage = null;
                    break;
            }

            mCurrentState = CreateState.idle_playback;
            updateUi();
            return;
        }

        Resources res = mActivity.getResources();
        String timeStr = "";

        if (t < 60)
            timeStr = res.getQuantityString(R.plurals.seconds_available, (int) t, t).toString();
        else if (t < 300)
            timeStr = res.getQuantityString(R.plurals.minutes_available, (int) (t / 60 + 1),
                    (t / 60 + 1)).toString();

        txtRecordStatus.setText(timeStr);

    }

    public SoundCloudApplication.RecordListener recListener = new com.soundcloud.android.SoundCloudApplication.RecordListener() {
        @Override
        public void onFrameUpdate(float maxAmplitude) {
            synchronized (this) {
                mPowerGauge.updateAmplitude(maxAmplitude);
                mPowerGauge.postInvalidate();
                onRecProgressUpdate((int) mRecordFile.length());
            }
        }

    };

    private void stopRecording() {
        mActivity.getSoundCloudApplication().setRecordListener(null);
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        try {
            (mActivity).getCreateService().stopRecording();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // disable actions during processing and playback preparation
        btnAction.setEnabled(false);
    }

    private void clearPlaybackTrack() {
        if (mPlaybackTask != null) {
            if (!CloudUtils.isTaskFinished(mPlaybackTask))
                mPlaybackTask.stopPlayback();
            mPlaybackTask.cancel(true);
        }

        if (playbackTrack != null) {
            playbackTrack.release();
            playbackTrack = null;
        }
        mProgressBar.setProgress(0);
    }

    private void startPlayback() {
        mPlaybackLength = (int) Math.floor(mRecordFile.length() / (44100 * 2));

        if (playbackTrack != null) {
            clearPlaybackTrack();
        }

        mProgressBar.setMax((int) mRecordFile.length());

        mPlaybackTask = new PCMPlaybackTask(mRecordFile);
        mPlaybackTask.setPlaybackListener(this);
        mPlaybackTask.execute();

        isPlayingBack = true;
        // startProgressTracker();

    }

    private void pausePlayback() {
        // playbackTrack.pause();
        mPlaybackTask.stopPlayback();
        // mPlaybackTask.
        isPlayingBack = false;

    }

    private void startUpload() {
        if ((mActivity).getCreateService() == null)
            return;

        Boolean uploading = true;
        try {
            uploading = (mActivity).getCreateService().isUploading();
            if (uploading)
                mActivity.showToast(R.string.wait_for_upload_to_finish);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (uploading) {
            mCurrentState = CreateState.idle_upload;
            updateUi();
            return;
        }

        Time time = new Time();
        time.setToNow();

        // Calendar header
        String dayOfWeek = DateUtils.getDayOfWeekString(time.weekDay + 1, DateUtils.LENGTH_LONG)
                .toLowerCase();

        String title = "";
        String oggFilename = "";
        if (!TextUtils.isEmpty(mWhatText.getText().toString())
                && !TextUtils.isEmpty(mWhereText.getText().toString()))
            oggFilename = title = mWhatText.getText().toString() + " at "
                    + mWhereText.getText().toString();
        else if (!TextUtils.isEmpty(mWhatText.getText().toString()))
            oggFilename = title = mWhatText.getText().toString();
        else if (!TextUtils.isEmpty(mWhereText.getText().toString()))
            oggFilename = title = mWhereText.getText().toString();
        else {
            title = "recording on " + dayOfWeek;
            oggFilename = "recording_on_"
                    + android.text.format.DateFormat.format("yyyy-MM-dd-hh-mm-ss",
                            new java.util.Date());
        }

        oggFilename += ".ogg";

        HashMap<String, String> trackdata = new HashMap<String, String>();

        final boolean privateUpload = mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
        trackdata.put("track[sharing]", privateUpload ? "private" : "public");
        if (!privateUpload) {
            for (int id : mConnectionList.postToServiceIds()) {
                trackdata.put("post_to[][id]", String.valueOf(id));
            }
        }

        trackdata.put("pcm_path", mRecordFile.getAbsolutePath());
        trackdata.put("track[title]", title);
        trackdata.put("track[tag_list]", "soundcloud:source=web-record");
        trackdata.put("ogg_filename", oggFilename);

        if (!TextUtils.isEmpty(mArtworkUri)) {
            trackdata.put("artwork_path", mArtworkUri);
            trackdata.put("artwork_in_sample_size", Integer.toString(mArtworkInSampleSize));
        }

        try {
            (mActivity).getCreateService().uploadTrack(trackdata);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mWhereText.setText("");
        mWhatText.setText("");
        clearArtwork();
    }


    private OnFocusChangeListener txtFocusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            InputMethodManager mgr = (InputMethodManager) mActivity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (hasFocus == false) {

                if (!TextUtils.isEmpty(((TextView) v).getText().toString()))
                    ((TextView) v).setText(CloudUtils.toTitleCase(((TextView) v).getText()
                            .toString()));

                if (mgr != null)
                    mgr.hideSoftInputFromWindow(mWhatText.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    };

    @Override
    public void onPlayComplete(boolean result) {
        mProgressBar.setProgress(0);
        if (mCurrentState != CreateState.idle_upload) {
            mCurrentState = CreateState.idle_playback;
            updateUi();
        }
    }

    private Long pcmTime;

    @Override
    public void onPlayProgressUpdate(int position) {
        pcmTime = CloudUtils.getPCMTime(position, REC_SAMPLE_RATE, REC_CHANNELS,
                REC_BITS_PER_SAMPLE);
        mProgressBar.setMax((int) mRecordFile.length());

        if (position >= mRecordFile.length()) {
            mPlaybackTask.stopPlayback();
            mChrono.setText(mCurrentDurationString + " / " + mCurrentDurationString);
        } else {
            mProgressBar.setProgress(position);
            mChrono.setText(CloudUtils.makeTimeString(pcmTime < 3600000 ? mDurationFormatShort
                    : mDurationFormatLong, pcmTime)
                    + " / " + mCurrentDurationString);

        }
    }

    public void onRecProgressUpdate(int position) {
        pcmTime = CloudUtils.getPCMTime(position, REC_SAMPLE_RATE, REC_CHANNELS,
                REC_BITS_PER_SAMPLE);
        mChrono.setText(CloudUtils.makeTimeString(pcmTime < 3600000 ? mDurationFormatShort
                : mDurationFormatLong, pcmTime));
        updateTimeRemaining();
    }

    private String getMinsSecsString(File file, int sampleRate, int channels, int bitsPerSample) {
        pcmTime = CloudUtils.getPCMTime(file.length(), REC_SAMPLE_RATE, REC_CHANNELS,
                REC_BITS_PER_SAMPLE);
        return CloudUtils.makeTimeString(pcmTime < 3600000 ? mDurationFormatShort
                : mDurationFormatLong, pcmTime);
    }

    private String getMinsSecsString(int position, int sampleRate, int channels, int bitsPerSample) {
        pcmTime = CloudUtils.getPCMTime(position, REC_SAMPLE_RATE, REC_CHANNELS,
                REC_BITS_PER_SAMPLE);
        return CloudUtils.makeTimeString(pcmTime < 3600000 ? mDurationFormatShort
                : mDurationFormatLong, pcmTime);
    }

}
