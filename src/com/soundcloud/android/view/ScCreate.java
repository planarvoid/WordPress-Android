
package com.soundcloud.android.view;

import static com.soundcloud.android.SoundCloudApplication.EMULATOR;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.activity.LocationPicker;
import com.soundcloud.android.task.PCMPlaybackTask;
import com.soundcloud.android.task.PCMPlaybackTask.PlaybackListener;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.utils.AnimUtils;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.record.CloudRecorder;
import com.soundcloud.utils.record.PowerGauge;
import com.soundcloud.utils.record.RemainingTimeCalculator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.AudioTrack;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScCreate extends ScTabView implements PlaybackListener {
    private ViewFlipper mViewFlipper, mSharingFlipper;

    private TextView txtInstructions, txtRecordStatus;

    private LinearLayout mFileLayout;

    private PowerGauge mPowerGauge;

    private SeekBar mProgressBar;

    private RadioGroup mRdoPrivacy;

    /* package */  RadioButton mRdoPrivate, mRdoPublic;
    /* package */  EditText mWhatText;
    /* package */  TextView mWhereText;
    
    private TextView mShareOptions;

    private ImageView mArtwork;
    private TextView mArtworkBg;
    
    private ImageButton btnAction;

    private File mRecordFile;
    private String mArtworkUri;
    private Bitmap mArtworkBitmap;

    private LazyActivity mActivity;

    private CreateState mLastState, mCurrentState;
    
    private int mRecProgressCounter = 0;

    private TextView mChrono;

    /* package */ ConnectionList mConnectionList;
    /* package */ AccessList mAccessList;
    /* package */ Time mRecordingStarted = new Time();

    private String mFourSquareVenueId;
    private double mLong, mLat;

    public void setPrivateShareEmails(String[] emails) {
        mAccessList.getAdapter().setAccessList(Arrays.asList(emails));
    }

    public void setWhere(String where, String id, double lng, double lat) {
        if (where != null) mWhereText.setTextKeepState(where);
        mFourSquareVenueId = id;
        mLong = lng;
        mLat = lat;
    }

    public enum CreateState {
        IDLE_RECORD, RECORD, IDLE_PLAYBACK, PLAYBACK, IDLE_UPLOAD, UPLOAD
    }

    private String mRecordErrorMessage;

    private PCMPlaybackTask mPlaybackTask;

    private AudioTrack playbackTrack;

    private String mDurationFormatLong;
    private String mDurationFormatShort;
    private String mCurrentDurationString;

    public static int REC_SAMPLE_RATE = 44100;
    public static int REC_CHANNELS = 2;
    public static int REC_BITS_PER_SAMPLE = 16;
    public static int REC_MAX_FILE_SIZE = 158760000; // 15 mins at 44100x16bitx2channels
    private static String UPLOAD_TEMP_PICTURE_PATH = CloudCache.EXTERNAL_CACHE_DIRECTORY + "tmp.bmp";
    
    WakeLock mWakeLock;

    private boolean mSampleInterrupted = false;
    private RemainingTimeCalculator mRemainingTimeCalculator;

    private Long pcmTime;

    public ScCreate(LazyActivity activity) {
        super(activity);
        mActivity = activity;

        if (EMULATOR) {
            // go straight to upload if running in emulator, since we can't record anyway
            mCurrentState = CreateState.IDLE_UPLOAD;
        } else {
            mCurrentState = CreateState.IDLE_RECORD;
        }

        LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.sc_create, this);

        mDurationFormatLong = mActivity.getString(R.string.durationformatlong);
        mDurationFormatShort = mActivity.getString(R.string.durationformatshort);

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * REC_CHANNELS * REC_BITS_PER_SAMPLE);

        initResourceRefs();

        updateUi(false);
        mRecordFile = new File(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/rec.mp4");
        mRecordErrorMessage = "";
    }

    /*
     * Whenever the UI is re-created (due f.ex. to orientation change) we have
     * to reinitialize references to the views.
     */
    private void initResourceRefs() {
        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);

        mFileLayout = (LinearLayout) findViewById(R.id.file_layout);
        FrameLayout mGaugeHolder = (FrameLayout) findViewById(R.id.gauge_holder);
        txtInstructions = (TextView) findViewById(R.id.txt_instructions);
        mProgressBar = (SeekBar) findViewById(R.id.progress_bar);

        txtRecordStatus = (TextView) findViewById(R.id.txt_record_status);

        mChrono = (TextView) findViewById(R.id.chronometer);
        mChrono.setVisibility(View.GONE);
        
        mShareOptions = (TextView) findViewById(R.id.txt_record_options);

        RelativeLayout mProgressFrame = (RelativeLayout) findViewById(R.id.progress_frame);
        mProgressFrame.setVisibility(View.GONE);

        btnAction = (ImageButton) findViewById(R.id.btn_action);
        btnAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onAction();
            }
        });

        findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.IDLE_RECORD;
                updateUi(true);
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.IDLE_UPLOAD;
                updateUi(true);
            }
        });

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.IDLE_PLAYBACK;
                updateUi(true);
            }
        });

        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.UPLOAD;
                updateUi(true);
            }
        });

        mArtwork = (ImageView) findViewById(R.id.artwork);
        mArtworkBg = (TextView) findViewById(R.id.txt_artwork_bg);
        
        
        mWhatText = (EditText) findViewById(R.id.what);
        mWhereText = (TextView) findViewById(R.id.where);

        mWhereText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, LocationPicker.class);
                intent.putExtra("name", ((TextView)v).getText().toString());
                mActivity.startActivityForResult(intent, LocationPicker.PICK_VENUE);
            }
        });

        mWhatText.addTextChangedListener(new Capitalizer(mWhatText));
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
                mActivity.showToast(R.string.cloud_upload_clear_artwork);
            }
        });
        
        mArtworkBg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(mActivity)
                .setMessage("Where would you like to get the image?").setPositiveButton(
                        "Take a new picture", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new
                                File(UPLOAD_TEMP_PICTURE_PATH)));
                                mActivity.startActivityForResult(i, CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE);
                            }
                        }).setNegativeButton("From the gallery", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                mActivity
                                       .startActivityForResult(intent, CloudUtils.RequestCodes.GALLERY_IMAGE_PICK);
                            }
                        }).create().show();
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

        mConnectionList = (ConnectionList) findViewById(R.id.connectionList);
        mConnectionList.setAdapter(
            new ConnectionList.Adapter(mActivity.getSoundCloudApplication())
            .load());

        mAccessList = (AccessList) findViewById(R.id.accessList);
        mAccessList.setAdapter(new AccessList.Adapter());
        mAccessList.getAdapter().setAccessList(null);
    }

    @Override
    public void onAuthenticated() {
        mConnectionList.getAdapter().loadIfNecessary();
    }

    @Override
    public void onReauthenticate() {
        mConnectionList.getAdapter().clear();
    }

    @Override
    public void onRefresh(boolean all) {
        super.onRefresh(all);
        onReauthenticate();
        onAuthenticated();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("createCurrentCreateState", mCurrentState.toString());
        state.putString("createWhatValue", mWhatText.getText().toString());
        state.putString("createWhereValue", mWhereText.getText().toString());
        state.putInt("createPrivacyValue", mRdoPrivacy.getCheckedRadioButtonId());

        if (!TextUtils.isEmpty(mArtworkUri)) {
            state.putString("createArtworkPath", mArtworkUri);
        }
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!TextUtils.isEmpty(state.getString("createCurrentCreateState"))) {
            mCurrentState = CreateState.valueOf(state.getString("createCurrentCreateState"));
            updateUi(false);
        }

        mWhatText.setText(state.getString("createWhatValue"));
        mWhereText.setText(state.getString("createWhereValue"));

        if (state.getInt("createPrivacyValue") == R.id.rdo_private) {
            mRdoPrivate.setChecked(true);
        } else {
            mRdoPublic.setChecked(true);
        }

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            setPickedImage(state.getString("createArtworkPath"));
        }

        super.onRestoreInstanceState(state);
    }

    public void onRecordingError() {
        mSampleInterrupted = true;
        mRecordErrorMessage = getResources().getString(R.string.error_recording_message);
        mCurrentState = CreateState.IDLE_RECORD;
        updateUi(true);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");
        try {
            if (mActivity.getCreateService() != null && mActivity.getCreateService().isUploading()) {
                mCurrentState = CreateState.UPLOAD;
            } else if (mCurrentState == CreateState.UPLOAD) {
                mCurrentState = CreateState.IDLE_RECORD;
            } else if (mCurrentState == null) {

                if (!mRecordFile.exists()) {
                    mCurrentState = CreateState.IDLE_RECORD;
                } else {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            mCurrentState = CreateState.IDLE_RECORD;
        }
        updateUi(false);
    }
    
    @Override
    public void onDestroy() {
        super.onStart();

        clearArtwork();
        updateUi(false);
    }

    public void unlock(boolean finished) {
        // not currently uploading anything, so allow recording
        if (mCurrentState == CreateState.UPLOAD) {
            mCurrentState =  finished ? CreateState.IDLE_RECORD : CreateState.IDLE_PLAYBACK;
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
        try {
            Options opt = CloudUtils.determineResizeOptions(new File(imageUri),  (int) getContext().getResources().getDisplayMetrics().density * 100,(int)
                    getContext().getResources().getDisplayMetrics().density * 100);
            mArtworkUri = imageUri;

            if (mArtworkBitmap != null)
                CloudUtils.clearBitmap(mArtworkBitmap);
            
            Matrix mat = new Matrix();
            mArtwork.setImageMatrix(mat);
            
            Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            try {
                
                mArtworkBitmap = BitmapFactory.decodeFile(mArtworkUri, sampleOpt);
                mArtwork.setImageBitmap(mArtworkBitmap);
                mArtwork.setVisibility(View.VISIBLE);
            } catch (Exception e){
                //temp
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
    }
    
    public void setTakenImage() {
        try {
            Log.i(TAG,"artwork bg " + mArtworkBg.getWidth());
            ExifInterface exif = new ExifInterface(UPLOAD_TEMP_PICTURE_PATH);
            Log.i(TAG,"orientation " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            String tagOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            
            Options opt = CloudUtils.determineResizeOptions(new File(UPLOAD_TEMP_PICTURE_PATH), (int) getContext().getResources().getDisplayMetrics().density * 100,(int)
                    getContext().getResources().getDisplayMetrics().density * 100);
            mArtworkUri = UPLOAD_TEMP_PICTURE_PATH;
            
            Matrix mat = new Matrix();
            
            if (TextUtils.isEmpty(tagOrientation) && Integer.parseInt(tagOrientation) >= 6){
                mat.postRotate(90);
            } 
            
            mArtwork.setImageMatrix(mat);

            if (mArtworkBitmap != null)
                CloudUtils.clearBitmap(mArtworkBitmap);

            try {
                Options sampleOpt = new BitmapFactory.Options();
                sampleOpt.inSampleSize = opt.inSampleSize;
                
                mArtworkBitmap = BitmapFactory.decodeFile(mArtworkUri, sampleOpt);
                mArtwork.setImageBitmap(mArtworkBitmap);
                mArtwork.setVisibility(View.VISIBLE);
            }  catch (Exception e){
                //temp
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }
    }

    public void clearArtwork() {
        mArtworkUri = null;
        mArtwork.setVisibility(View.GONE);

        if (mArtworkBitmap != null)
            CloudUtils.clearBitmap(mArtworkBitmap);
    }


    /*** State Handling ***/
    private void onAction() {
        switch (mCurrentState) {
            case IDLE_RECORD:   mCurrentState = CreateState.RECORD; break;
            case RECORD:        mCurrentState = CreateState.IDLE_PLAYBACK; break;
            case IDLE_PLAYBACK: mCurrentState = CreateState.PLAYBACK; break;
            case PLAYBACK:      mCurrentState = CreateState.IDLE_PLAYBACK; break;
        }
        updateUi(true);
    }

    private void updateUi(boolean takeAction) {
        Log.i(TAG, "Update Soundcloud Create state: " + mCurrentState + " | take action: "
                + takeAction);
        switch (mCurrentState) {
            case IDLE_RECORD:
                goToView(0);
                if (takeAction) clearPlaybackTrack();

                btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(
                        R.drawable.btn_rec_states));
                mFileLayout.setVisibility(View.GONE);

                if (TextUtils.isEmpty(mRecordErrorMessage)) {
                    txtRecordStatus.setText(getResources().getString(
                            R.string.cloud_recorder_experimental));
                } else {
                    txtRecordStatus.setText(mRecordErrorMessage);
                }

                txtRecordStatus.setVisibility(View.VISIBLE);

                mChrono.setVisibility(View.GONE);

                mProgressBar.setVisibility(View.GONE);
                mPowerGauge.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.VISIBLE);
                break;

            case RECORD:
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

                if (takeAction) startRecording();
                break;

            case IDLE_PLAYBACK:
                goToView(0);

                calculateTotalProgress();
                mChrono.setText(mCurrentDurationString);

                if (takeAction) {
                    switch (mLastState) {
                        case RECORD: stopRecording(); break;
                        case PLAYBACK:
                            mPlaybackTask.stopPlayback();
                            break;
                    }
                }

                btnAction.setBackgroundDrawable(mActivity.getResources().getDrawable(
                        R.drawable.btn_rec_play_states));
                txtInstructions.setVisibility(View.GONE);
                mFileLayout.setVisibility(View.VISIBLE);
                mPowerGauge.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mChrono.setVisibility(View.VISIBLE);
                txtRecordStatus.setVisibility(View.GONE);

                break;

            case PLAYBACK:
                goToView(0);
                calculateTotalProgress();
                txtRecordStatus.setVisibility(View.GONE);
                txtInstructions.setVisibility(View.GONE);
                mFileLayout.setVisibility(View.VISIBLE);
                mPowerGauge.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.VISIBLE);
                mChrono.setVisibility(View.VISIBLE);
                btnAction.setBackgroundDrawable(mActivity.getResources()
                        .getDrawable(R.drawable.btn_rec_stop_states));

                if (takeAction) startPlayback();
                break;

            case IDLE_UPLOAD:
                goToView(1);
                Log.i(TAG,"Idle Upload " + mRdoPrivacy.getCheckedRadioButtonId());
                if (mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private){
                    mShareOptions.setText(mActivity.getResources().getString(R.string.cloud_uploader_share_options_private));
                } else {
                    mShareOptions.setText(mActivity.getResources().getString(R.string.cloud_uploader_share_options_public));
                }
                break;

            case UPLOAD:
                goToView(2);
                clearPlaybackTrack();
                flipRight();
                if (takeAction) startUpload();
                break;
        }

        mLastState = mCurrentState;
        btnAction.setEnabled(true);
    }

    private void calculateTotalProgress() {
        mCurrentDurationString = getMinsSecsString(mRecordFile);
    }

    // View Flipping
    private void goToView(int child) {
        Log.d(TAG, "goto view" + child + " current:" + mViewFlipper.getDisplayedChild());

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
        if (mViewFlipper.getDisplayedChild() != 2) {
            mViewFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
            mViewFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
            mViewFlipper.showNext();
        }
    }

    private void flipLeft() {
        if (mViewFlipper.getDisplayedChild() != 0) {
            mViewFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
            mViewFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
            mViewFlipper.showPrevious();
        }
    }

    private void startRecording() {
        mActivity.forcePause();

        mRecordErrorMessage = "";
        mSampleInterrupted = false;
        
        mRecProgressCounter = 0;

        mRemainingTimeCalculator.reset();
        mPowerGauge.clear();

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_storage_is_full);
        }

        if (mSampleInterrupted) {
            mCurrentState = CreateState.IDLE_RECORD;
            updateUi(true);
        } else {
            mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * REC_CHANNELS * REC_BITS_PER_SAMPLE);
            if (REC_MAX_FILE_SIZE != -1) {
                mRemainingTimeCalculator.setFileSizeLimit(mRecordFile, REC_MAX_FILE_SIZE);
            }

            mActivity.setRequestedOrientation(mActivity.getResources().getConfiguration().orientation);
            try {
                (mActivity).getCreateService().startRecording(mRecordFile.getAbsolutePath());
                mRecordingStarted.setToNow();
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }

            mActivity.getSoundCloudApplication().setRecordListener(recListener);
        }
    }

    /*
     * Called when we're in recording state. Find out how much longer we can go
     * on recording. If it's under 5 minutes, we display a count-down in the UI.
     * If we've run out of time, stop the recording.
     */
    private void updateTimeRemaining() {
        // adding 2 seconds to make up for lag
        final long t = mRemainingTimeCalculator.timeRemaining() + 2;
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

            mCurrentState = CreateState.IDLE_PLAYBACK;
            updateUi(true);
            return;
        }

        Resources res = mActivity.getResources();
        String timeStr = "";

        if (t < 60) {
            timeStr = res.getQuantityString(R.plurals.seconds_available, (int) t, t);
        } else if (t < 300) {
            timeStr = res.getQuantityString(R.plurals.minutes_available, (int) (t / 60 + 1),
                    (t / 60 + 1));
        }
        txtRecordStatus.setText(timeStr);

    }

    public SoundCloudApplication.RecordListener recListener = new SoundCloudApplication.RecordListener() {
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
            Log.e(TAG, "error", e);
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
        if (playbackTrack != null) {
            clearPlaybackTrack();
        }

        mProgressBar.setMax((int) mRecordFile.length());
        mPlaybackTask = new PCMPlaybackTask(mRecordFile);
        mPlaybackTask.setPlaybackListener(this);
        mPlaybackTask.execute();

    }

    void startUpload() {
        if (mActivity.getCreateService() == null) return;

        boolean uploading;
        try {
            uploading = (mActivity).getCreateService().isUploading();
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            uploading = true;
        }

        if (!uploading) {
            final boolean privateUpload = mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
            final Map<String, Object> data = new HashMap<String, Object>();
            data.put(CloudAPI.Params.SHARING, privateUpload ? CloudAPI.Params.PRIVATE : CloudAPI.Params.PUBLIC);


            if (!privateUpload) {
                Log.v(TAG, "public track upload");

                final List<Integer> serviceIds = mConnectionList.postToServiceIds();

                 if (!serviceIds.isEmpty()) {
                    data.put(CloudAPI.Params.SHARING_NOTE, generateSharingNote());
                    data.put(CloudAPI.Params.POST_TO, serviceIds);
                 } else {
                    data.put(CloudAPI.Params.POST_TO_EMPTY, "");
                 }
            } else {
                Log.v(TAG, "private track upload");

                final List<String> sharedEmails = mAccessList.getAdapter().getAccessList();
                if (sharedEmails != null && !sharedEmails.isEmpty()) {
                    data.put(CloudAPI.Params.SHARED_EMAILS, sharedEmails);
                }
            }

            final String title = generateTitle();
            data.put(CloudAPI.Params.TITLE, title);
            data.put(CloudAPI.Params.TYPE, "recording");

            // add machine tags
            List<String> tags = new ArrayList<String>();
            tags.add("soundcloud:source=web-record");
            if (mFourSquareVenueId != null) tags.add("foursquare:venue="+mFourSquareVenueId);
            if (mLat  != 0) tags.add("geo:lat="+mLat);
            if (mLong != 0) tags.add("geo:long="+mLong);
            data.put(CloudAPI.Params.TAG_LIST, TextUtils.join(" ", tags));

            data.put(UploadTask.Params.OGG_FILENAME, CloudUtils.getCacheFilePath(this.getContext(), generateFilename(title)));
            data.put(UploadTask.Params.PCM_PATH, mRecordFile.getAbsolutePath());

            if (!TextUtils.isEmpty(mArtworkUri))
                data.put(UploadTask.Params.ARTWORK_PATH, mArtworkUri);

            try {
                (mActivity).getCreateService().uploadTrack(data);
            } catch (RemoteException ignored) {
                Log.e(TAG, "error", ignored);
            } finally {
                mWhereText.setText("");
                mWhatText.setText("");
                clearArtwork();
            }
        } else {
            mActivity.showToast(R.string.wait_for_upload_to_finish);
            mCurrentState = CreateState.IDLE_UPLOAD;
            updateUi(true);
        }
    }

    private String dateString() {
        String day = DateUtils.getDayOfWeekString(mRecordingStarted.weekDay + 1, DateUtils.LENGTH_LONG);
        String dayTime;
        if (mRecordingStarted.hour <= 12) {
            dayTime = "morning";
        } else if (mRecordingStarted.hour <= 17) {
            dayTime = "afternoon";
        } else if (mRecordingStarted.hour <= 21) {
           dayTime = "evening";
        } else {
           dayTime = "night";
        }

        return day + " " + dayTime;
    }

    private String generateTitle() {
        String title;
        if (mWhatText.length() > 0 && mWhereText.length() > 0) {
            title = mWhatText.getText() + " at " + mWhereText.getText();
        } else if (mWhatText.length() > 0) {
            title = mWhatText.getText().toString();
        } else if (mWhereText.length() > 0) {
            title = mWhereText.getText().toString();
        } else {
            title = "recording on " + dateString();
        }
        return title;
    }

    private String generateFilename(String title) {
        return String.format("%s_%s.ogg", title,
                DateFormat.format("yyyy-MM-dd-hh-mm-ss", mRecordingStarted.toMillis(false))
        );
    }

    private String generateSharingNote() {
        String note;
        if (mWhatText.length() > 0) {
            if (mWhereText.length() > 0) {
                note = String.format("%s at %s", mWhatText.getText(), mWhereText.getText());
            } else {
                note = mWhatText.getText().toString();
            }
        } else {
            if (mWhereText.length() > 0) {
                note = String.format("Sounds at %s", mWhereText.getText());
            } else {
                note = String.format("Sounds from %s", dateString());
            }
        }
        return note;
    }

    public void onPlayComplete(boolean result) {
        mProgressBar.setProgress(0);
        if (mCurrentState != CreateState.IDLE_UPLOAD) {
            mCurrentState = CreateState.IDLE_PLAYBACK;
            updateUi(true);
        }
    }


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
        if (mRecProgressCounter % (1000 / CloudRecorder.TIMER_INTERVAL) == 0){
            pcmTime = CloudUtils.getPCMTime(position, REC_SAMPLE_RATE, REC_CHANNELS,
                REC_BITS_PER_SAMPLE);
            mChrono.setText(CloudUtils.makeTimeString(pcmTime < 3600000 ? mDurationFormatShort
                : mDurationFormatLong, pcmTime));
            updateTimeRemaining();
            mRecProgressCounter = 0;
        } 
        mRecProgressCounter++;

    }

    private String getMinsSecsString(File file) {
        pcmTime = CloudUtils.getPCMTime(file.length(), REC_SAMPLE_RATE, REC_CHANNELS,
                REC_BITS_PER_SAMPLE);
        return CloudUtils.makeTimeString(pcmTime < 3600000 ? mDurationFormatShort
                : mDurationFormatLong, pcmTime);
    }

    public static class Capitalizer implements TextWatcher {
        private TextView text;
        public Capitalizer(TextView text) {
            this.text = text;
        }

        public void afterTextChanged(Editable s) {
            if (s.length() == 1
            && !s.toString().toUpperCase().contentEquals(s.toString())) {
                text.setTextKeepState(s.toString().toUpperCase());
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
