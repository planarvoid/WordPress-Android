package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.EMULATOR;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.task.PCMPlaybackTask;
import com.soundcloud.android.task.PCMPlaybackTask.PlaybackListener;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.view.AccessList;
import com.soundcloud.android.view.ConnectionList;
import com.soundcloud.utils.AnimUtils;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.record.CloudRecorder;
import com.soundcloud.utils.record.PowerGauge;
import com.soundcloud.utils.record.RemainingTimeCalculator;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.AudioTrack;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScCreate extends ScActivity implements PlaybackListener {
    private static final String TAG = "ScCreate";

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

    private ImageButton btnAction;

    private File mRecordFile;
    private String mArtworkUri;
    private Bitmap mArtworkBitmap;

    /* package */ ICloudCreateService mCreateService;
    private CreateState mLastState, mCurrentState;

    private int mRecProgressCounter = 0;

    private TextView mChrono;

    /* package */ ConnectionList mConnectionList;
    /* package */ AccessList mAccessList;
    /* package */ Time mRecordingStarted = new Time();

    private String mFourSquareVenueId;
    private double mLong, mLat;

    private boolean mExternalUpload;

    private ServiceConnection createOsc = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mCreateService = (ICloudCreateService) binder;
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };


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

    private boolean mSampleInterrupted = false;
    private RemainingTimeCalculator mRemainingTimeCalculator;

    private Long pcmTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // go straight to upload if running in emulator, since we can't record anyway
        mCurrentState = EMULATOR ? CreateState.IDLE_UPLOAD : CreateState.IDLE_RECORD;

        setContentView(R.layout.sc_create);

        initResourceRefs();

        updateUi(false);

        mRecordFile = new File(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/rec.pcm");
        mRecordErrorMessage = "";

        // XXX do in manifest
        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.RECORD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        this.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));
    }

    @Override
    public void onStart() {
        super.onStart();
        CloudUtils.bindToService(this, CloudCreateService.class, createOsc);

        File streamFile = null;
        if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            Uri stream = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if ("file".equals(stream.getScheme())) {
                streamFile = new File(stream.getPath());
            }
        }

        if (streamFile != null && streamFile.exists()) {
            mRecordFile = streamFile;
            mCurrentState = CreateState.IDLE_UPLOAD;
            mExternalUpload = true;
        } else {
            try {
                if (mCreateService != null && mCreateService.isUploading()) {
                    mCurrentState = CreateState.UPLOAD;
                } else if (mCurrentState == CreateState.UPLOAD) {
                    mCurrentState = CreateState.IDLE_RECORD;
                } else if (mCurrentState == CreateState.IDLE_RECORD && mRecordFile.exists()) {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
                mCurrentState = CreateState.IDLE_RECORD;
            }
        }
        updateUi(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionList.getAdapter().loadIfNecessary();
    }

    @Override
    public void onDestroy() {
        super.onStart();

        this.unregisterReceiver(mUploadStatusListener);
        clearArtwork();
        updateUi(false);
    }

    /*
    * Whenever the UI is re-created (due f.ex. to orientation change) we have
    * to reinitialize references to the views.
    */
    private void initResourceRefs() {
        mDurationFormatLong = getString(R.string.durationformatlong);
        mDurationFormatShort = getString(R.string.durationformatshort);

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * REC_CHANNELS * REC_BITS_PER_SAMPLE);

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
                if (mExternalUpload) {
                    setResult(RESULT_CANCELED);
                    finish();
                } else {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                    updateUi(true);
                }
            }
        });

        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCurrentState = CreateState.UPLOAD;
                updateUi(true);

                if (mExternalUpload) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });

        mArtwork = (ImageView) findViewById(R.id.artwork);
        TextView mArtworkBg = (TextView) findViewById(R.id.txt_artwork_bg);


        mWhatText = (EditText) findViewById(R.id.what);
        mWhereText = (TextView) findViewById(R.id.where);

        mWhereText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScCreate.this, LocationPicker.class);
                intent.putExtra("name", ((TextView)v).getText().toString());
                startActivityForResult(intent, LocationPicker.PICK_VENUE);
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
                //if (checkedId == R.id.rdo_private){
                  //  mShareOptions.setText(getString(R.string.cloud_uploader_share_options_private));
                //} else {
                  //  mShareOptions.setText(getString(R.string.cloud_uploader_share_options_public));
                //}
            }
        });

        mArtwork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showToast(R.string.cloud_upload_clear_artwork);
            }
        });

        mArtworkBg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(ScCreate.this)
                        .setMessage("Where would you like to get the image?").setPositiveButton(
                        "Take a new picture", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new
                                        File(UPLOAD_TEMP_PICTURE_PATH)));
                                startActivityForResult(i, CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE);
                            }
                        }).setNegativeButton("Use existing image", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CloudUtils.RequestCodes.GALLERY_IMAGE_PICK);
                    }
                }).create().show();
            }
        });

        mArtwork.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                clearArtwork();
                return true;
            }
        });

        mRemainingTimeCalculator = new RemainingTimeCalculator();
        mPowerGauge = new PowerGauge(this);
        mGaugeHolder.addView(mPowerGauge);

        mConnectionList = (ConnectionList) findViewById(R.id.connectionList);
        mConnectionList.setAdapter(new ConnectionList.Adapter(this.getSoundCloudApplication()));

        mAccessList = (AccessList) findViewById(R.id.accessList);
        mAccessList.setAdapter(new AccessList.Adapter());
        mAccessList.getAdapter().setAccessList(null);

        mAccessList.getAdapter().registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                findViewById(R.id.btn_add_emails).setVisibility(
                    mAccessList.getAdapter().getCount() > 0 ? View.GONE : View.VISIBLE
                );
            }
        });

        findViewById(R.id.btn_add_emails).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> accessList = mAccessList.getAdapter().getAccessList();
                Intent intent = new Intent(ScCreate.this, EmailPicker.class);
                if (accessList != null) {
                    intent.putExtra(EmailPicker.BUNDLE_KEY, accessList.toArray(new String[accessList.size()]));
                    if (v instanceof TextView) {
                        intent.putExtra(EmailPicker.SELECTED, ((TextView) v).getText());
                    }
                }
                startActivityForResult(intent, EmailPicker.PICK_EMAILS);
            }
        });
    }



    @Override
    public void onReauthenticate() {
        onRefresh();
    }

    @Override
    public void onRefresh() {
        mConnectionList.getAdapter().clear();
        mConnectionList.getAdapter().loadIfNecessary();
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



    public void unlock(boolean finished) {
        // not currently uploading anything, so allow recording
        if (mCurrentState == CreateState.UPLOAD) {
            mCurrentState =  finished ? CreateState.IDLE_RECORD : CreateState.IDLE_PLAYBACK;
            updateUi(false);
        }
    }

    public void setPickedImage(String imageUri) {
        try {

            Options opt = CloudUtils.determineResizeOptions(new File(imageUri),
                    (int) getResources().getDisplayMetrics().density * 100,
                    (int) getResources().getDisplayMetrics().density * 100);

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
            ExifInterface exif = new ExifInterface(UPLOAD_TEMP_PICTURE_PATH);
            String tagOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

            Options opt = CloudUtils.determineResizeOptions(new File(UPLOAD_TEMP_PICTURE_PATH),
                    (int) getResources().getDisplayMetrics().density * 100,
                    (int) getResources().getDisplayMetrics().density * 100);
            mArtworkUri = UPLOAD_TEMP_PICTURE_PATH;

            Matrix mat = new Matrix();

            if (TextUtils.isEmpty(tagOrientation) && Integer.parseInt(tagOrientation) >= 6){
                mat.postRotate(90);
            }

            mArtwork.setImageMatrix(mat);

            if (mArtworkBitmap != null)
                CloudUtils.clearBitmap(mArtworkBitmap);

            BitmapFactory.Options resample = new BitmapFactory.Options();
            resample.inSampleSize = opt.inSampleSize;

            mArtworkBitmap = BitmapFactory.decodeFile(mArtworkUri, resample);
            mArtwork.setImageBitmap(mArtworkBitmap);
            mArtwork.setVisibility(View.VISIBLE);
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

                btnAction.setBackgroundDrawable(getResources().getDrawable(
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

                btnAction.setBackgroundDrawable(getResources().getDrawable(
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

                btnAction.setBackgroundDrawable(getResources().getDrawable(
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
                btnAction.setBackgroundDrawable(getResources()
                        .getDrawable(R.drawable.btn_rec_stop_states));

                if (takeAction) startPlayback();
                break;

            case IDLE_UPLOAD:
                goToView(1);
                /*if (mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private){
                    mShareOptions.setText(getString(R.string.cloud_uploader_share_options_private));
                } else {
                    mShareOptions.setText(getString(R.string.cloud_uploader_share_options_public));
                }*/
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
        pause(true);

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

            setRequestedOrientation(getResources().getConfiguration().orientation);
            try {
                mCreateService.startRecording(mRecordFile.getAbsolutePath());
                mRecordingStarted.setToNow();
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }

            getSoundCloudApplication().setRecordListener(recListener);
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

        Resources res = getResources();
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
        getSoundCloudApplication().setRecordListener(null);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        try {
            mCreateService.stopRecording();
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
        if (mCreateService == null) return;

        boolean uploading;
        try {
            uploading = mCreateService.isUploading();
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

            data.put(UploadTask.Params.OGG_FILENAME, CloudUtils.getCacheFilePath(this, generateFilename(title)));
            data.put(UploadTask.Params.SOURCE_PATH, mRecordFile.getAbsolutePath());

            if (mExternalUpload) data.put(UploadTask.Params.EXTERNAL, mExternalUpload);

            if (!TextUtils.isEmpty(mArtworkUri)) {
                data.put(UploadTask.Params.ARTWORK_PATH, mArtworkUri);
            }

            try {
                mCreateService.uploadTrack(data);
            } catch (RemoteException ignored) {
                Log.e(TAG, "error", ignored);
            } finally {
                mWhereText.setText("");
                mWhatText.setText("");
                clearArtwork();
            }
        } else {
            showToast(R.string.wait_for_upload_to_finish);
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

    private BroadcastReceiver mUploadStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(CloudCreateService.UPLOAD_ERROR)
                    || action.equals(CloudCreateService.UPLOAD_CANCELLED))
                onCreateComplete(false);
            else if (action.equals(CloudCreateService.UPLOAD_SUCCESS))
                onCreateComplete(true);
            else if (action.equals(CloudCreateService.RECORD_ERROR))
                onRecordingError();
        }
    };



    protected void onCreateComplete(boolean success) {
        unlock(success);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {

        switch (requestCode) {
            case CloudUtils.RequestCodes.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = result.getData();
                    String[] filePathColumn = {
                            MediaStore.MediaColumns.DATA
                    };

                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null,
                            null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();


                    setPickedImage(filePath);


                }
                break;

            case EmailPicker.PICK_EMAILS:
                if (resultCode == RESULT_OK &&result != null && result.hasExtra(EmailPicker.BUNDLE_KEY)) {
                    String[] emails = result.getExtras().getStringArray(EmailPicker.BUNDLE_KEY);
                    if (emails != null) {
                        setPrivateShareEmails(emails);
                    }
                }
                break;
            case LocationPicker.PICK_VENUE:
                if (resultCode == RESULT_OK && result != null && result.hasExtra("name")) {
                    setWhere(result.getStringExtra("name"),
                            result.getStringExtra("id"),
                            result.getDoubleExtra("longitude", 0),
                            result.getDoubleExtra("latitude", 0));
                }
                break;
            case Connect.MAKE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    boolean success = result.getBooleanExtra("success", false);
                    String msg = getString(
                            success ? R.string.connect_success : R.string.connect_failure,
                            result.getStringExtra("service"));
                    Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();

                    if (success) mConnectionList.getAdapter().load();
                }
        }
    }


    protected void cancelCurrentUpload() {
        try {
            mCreateService.cancelUpload();
        } catch (RemoteException e) {
            setException(e);
            handleException();
        }
    }

}
