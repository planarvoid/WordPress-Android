package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.EMULATOR;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.view.AccessList;
import com.soundcloud.android.view.ConnectionList;
import com.soundcloud.utils.AnimUtils;
import com.soundcloud.utils.CloudCache;
import com.soundcloud.utils.record.CloudRecorder.Profile;
import com.soundcloud.utils.record.PowerGauge;
import com.soundcloud.utils.record.RemainingTimeCalculator;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
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
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScCreate extends ScActivity {
    private static final String TAG = "ScCreate";

    private ViewFlipper mViewFlipper, mSharingFlipper;

    private TextView txtInstructions, txtRecordStatus, txtUploadingStatus;

    private LinearLayout mFileLayout;

    private PowerGauge mPowerGauge;

    private SeekBar mProgressBar;

    private RadioGroup mRdoPrivacy;

    /* package */  RadioButton mRdoPrivate, mRdoPublic;
    /* package */  EditText mWhatText;
    /* package */  TextView mWhereText;

    private ImageView mArtwork;

    private ImageButton btnAction;

    private File mRecordDir;
    private File mRecordFile;

    private String mArtworkUri;
    private Bitmap mArtworkBitmap;

    private CreateState mLastState, mCurrentState;

    private long mLastDisplayedTime = 0;

    private TextView mChrono;

    private long mLastSeekEventTime;

    /* package */ ConnectionList mConnectionList;
    /* package */ AccessList mAccessList;
    /* package */ Time mRecordingStarted = new Time();

    private String mFourSquareVenueId;
    private double mLong, mLat;

    boolean mExternalUpload;
    private int mAudioProfile;

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

    private MediaPlayer mPlayer;

    private String mDurationFormatLong;
    private String mDurationFormatShort;
    private String mCurrentDurationString;

    public static int REC_SAMPLE_RATE = 44100;
    public static int PCM_REC_CHANNELS = 1;
    public static int PCM_REC_BITS_PER_SAMPLE = 16;
    public static int PCM_REC_MAX_FILE_SIZE = -1;
    //public static int PCM_REC_MAX_FILE_SIZE = 158760000; // 15 mins at 44100x16bitx2channels

    private static String UPLOAD_TEMP_PICTURE_PATH = CloudCache.EXTERNAL_CACHE_DIRECTORY + "tmp.bmp";

    private boolean mSampleInterrupted = false;
    private RemainingTimeCalculator mRemainingTimeCalculator;

    private static final String RAW_PATTERN = "^.*\\.(2|pcm)$";
    private static final String COMPRESSED_PATTERN = "^.*\\.(0|1|mp4|ogg)$";
    private Pattern pattern;
    private Matcher matcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // go straight to upload if running in emulator, since we can't record anyway
        mCurrentState = EMULATOR ? CreateState.IDLE_UPLOAD : CreateState.IDLE_RECORD;

        setContentView(R.layout.sc_create);

        initResourceRefs();

        updateUi(false);

        mRecordDir = new File(CloudUtils.EXTERNAL_STORAGE_DIRECTORY + "/.rec/");
        if (!mRecordDir.exists()) mRecordDir.mkdirs();

        mRecordErrorMessage = "";

        // XXX do in manifest
        IntentFilter uploadFilter = new IntentFilter();
        uploadFilter.addAction(CloudCreateService.RECORD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_ERROR);
        uploadFilter.addAction(CloudCreateService.UPLOAD_CANCELLED);
        uploadFilter.addAction(CloudCreateService.UPLOAD_SUCCESS);
        this.registerReceiver(mUploadStatusListener, new IntentFilter(uploadFilter));

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionList.getAdapter().loadIfNecessary();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopPlayback();

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
        mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * PCM_REC_CHANNELS * PCM_REC_BITS_PER_SAMPLE);

        mViewFlipper = (ViewFlipper) findViewById(R.id.flipper);

        mFileLayout = (LinearLayout) findViewById(R.id.file_layout);
        FrameLayout mGaugeHolder = (FrameLayout) findViewById(R.id.gauge_holder);
        txtInstructions = (TextView) findViewById(R.id.txt_instructions);
        mProgressBar = (SeekBar) findViewById(R.id.progress_bar);
        mProgressBar.setOnSeekBarChangeListener(mSeekListener);

        txtRecordStatus = (TextView) findViewById(R.id.txt_record_status);
        txtUploadingStatus = (TextView) findViewById(R.id.txt_currently_uploading);

        mChrono = (TextView) findViewById(R.id.chronometer);
        mChrono.setVisibility(View.GONE);

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
    public void onCreateServiceBound() {
        super.onCreateServiceBound();

        File streamFile = null;
        if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            Uri stream = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if ("file".equals(stream.getScheme())) {
                streamFile = new File(stream.getPath());
            }
        }

        boolean takeAction = false;
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
                    takeAction = true;
                } else if (mCurrentState == CreateState.IDLE_RECORD) {

                    if (!mRecordDir.exists()) {
                        // can happen when there's no mounted sdcard
                        btnAction.setEnabled(false);
                    } else if (mRecordDir.list().length > 0) {
                        setRecordFile();

                        if (mRecordFile != null) {
                            mCurrentState = CreateState.IDLE_PLAYBACK;
                            loadPlaybackTrack();
                        } else {
                            // delete whatever is in the rec directory, we can't
                            // use it
                            takeAction = true;
                        }
                    }
                } // IDLE_RECORD
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
                mCurrentState = CreateState.IDLE_RECORD;
            }
        }
        updateUi(takeAction);
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
        state.putLong("recordingStarted", mRecordingStarted.toMillis(false));

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

        mRecordingStarted.set(state.getLong("recordingStarted"));
        mWhatText.setText(state.getString("createWhatValue"));
        mWhereText.setText(state.getString("createWhereValue"));

        if (state.getInt("createPrivacyValue") == R.id.rdo_private) {
            mRdoPrivate.setChecked(true);
        } else {
            mRdoPublic.setChecked(true);
        }

        if (!TextUtils.isEmpty(state.getString("createArtworkPath"))) {
            if (state.getString("createArtworkPath").contentEquals(UPLOAD_TEMP_PICTURE_PATH))
                setTakenImage(); //account for rotation
            else
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
            if (!finished){
                //recover record file
                setRecordFile();

                if (mRecordFile != null && mRecordFile.exists()){
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                    loadPlaybackTrack();
                    updateUi(false);
                    return;
                }
            }

            mCurrentState =  CreateState.IDLE_RECORD;
            updateUi(true);

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

    // TODO move this code into a helper class
    public void setTakenImage() {
        mArtworkUri = UPLOAD_TEMP_PICTURE_PATH;
        try {
            final int density = (int) (getResources().getDisplayMetrics().density * 100);
            Options opt = CloudUtils.determineResizeOptions(new File(UPLOAD_TEMP_PICTURE_PATH), density, density);

            ExifInterface exif = new ExifInterface(mArtworkUri);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            int degree = 0;
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                    default:
                        degree = 0;
                        break;
                }
            }
            if (mArtworkBitmap != null) CloudUtils.clearBitmap(mArtworkBitmap);

            Options sampleOpt = new BitmapFactory.Options();
            sampleOpt.inSampleSize = opt.inSampleSize;

            mArtworkBitmap = BitmapFactory.decodeFile(mArtworkUri, sampleOpt);

            Matrix m = new Matrix();
            float scale;
            float dx = 0, dy = 0;
            int vwidth = (int) (getResources().getDisplayMetrics().density * 100);

            if (mArtworkBitmap.getWidth() > mArtworkBitmap.getHeight()) {
                scale = (float) vwidth / (float) mArtworkBitmap.getHeight();
                dx = (vwidth - mArtworkBitmap.getWidth() * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) mArtworkBitmap.getWidth();
                dy = (vwidth - mArtworkBitmap.getHeight() * scale) * 0.5f;
            }

            m.setScale(scale, scale);
            m.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            //pivot point in the middle, may need to change this
            if (degree != 0) m.postRotate(90, vwidth / 2, vwidth / 2);

            mArtwork.setScaleType(ScaleType.MATRIX);
            mArtwork.setImageMatrix(m);

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
                if (takeAction) {
                    stopPlayback();
                    clearCurrentFiles();
                    mWhereText.setText("");
                    mWhatText.setText("");
                    clearArtwork();
                }

                btnAction.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.btn_rec_states));
                mFileLayout.setVisibility(View.GONE);

                if (!TextUtils.isEmpty(mRecordErrorMessage)) {
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

                if (takeAction) {
                    switch (mLastState) {
                        case RECORD: stopRecording(); break;
                        case PLAYBACK:
                            if (mPlayer.isPlaying()) mPlayer.pause();
                            break;
                    }
                }

                mChrono.setText(mCurrentDurationString);

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
                break;

            case UPLOAD:
                goToView(2);
                stopPlayback();
                flipRight();
                if (takeAction) startUpload();
                break;
        }

        mLastState = mCurrentState;
        btnAction.setEnabled(true);
    }

    // View Flipping
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

    private void clearCurrentFiles() {
        if (mRecordDir.exists()){
            for (File f : mRecordDir.listFiles()){
                f.delete();
            }
        }
        mRecordFile = null;
    }

    private void startRecording() {
        pause(true);

        mRecordErrorMessage = "";
        mSampleInterrupted = false;

        mLastDisplayedTime = 0;

        mRemainingTimeCalculator.reset();
        mPowerGauge.clear();

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_insert_sd_card);
        } else if (!mRemainingTimeCalculator.diskSpaceAvailable()) {
            mSampleInterrupted = true;
            mRecordErrorMessage = getResources().getString(R.string.record_storage_is_full);
        }

        final boolean hiQ = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("defaultRecordingQuality", "high")
            .contentEquals("high");

        mAudioProfile = hiQ ? Profile.best() : Profile.low();

        mRecordFile = new File(mRecordDir,"tmp." + mAudioProfile);

        if (mSampleInterrupted) {
            mCurrentState = CreateState.IDLE_RECORD;
            updateUi(true);
        } else {
            mRemainingTimeCalculator.setBitRate(REC_SAMPLE_RATE * PCM_REC_CHANNELS * PCM_REC_BITS_PER_SAMPLE);
            if (PCM_REC_MAX_FILE_SIZE != -1) {
                mRemainingTimeCalculator.setFileSizeLimit(mRecordFile, PCM_REC_MAX_FILE_SIZE);
            }

            setRequestedOrientation(getResources().getConfiguration().orientation);
            try {
                mCreateService.startRecording(mRecordFile.getAbsolutePath(), mAudioProfile);
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
        public void onFrameUpdate(float maxAmplitude, long elapsed) {
            synchronized (this) {
                mPowerGauge.updateAmplitude(maxAmplitude);
                mPowerGauge.postInvalidate();
                onRecProgressUpdate(elapsed);
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
        loadPlaybackTrack();
    }

    private void loadPlaybackTrack(){
        mPlayer.reset();

        try {
            FileInputStream fis = new FileInputStream(mRecordFile);
            mPlayer.setDataSource(fis.getFD());
            fis.close();

            mPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        mCurrentDurationString =  CloudUtils.makeTimeString(mDurationFormatShort,
                mPlayer.getDuration() / 1000);

        mProgressBar.setMax(mPlayer.getDuration());

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mProgressBar.setProgress(0);
                if (mCurrentState == CreateState.PLAYBACK) {
                    mCurrentState = CreateState.IDLE_PLAYBACK;
                    loadPlaybackTrack();
                    updateUi(true);
                }
            }
        });

    }

    private void startPlayback() {

        mPlayer.start();
        new Thread() {
            @Override
            public void run() {
                while (mPlayer.isPlaying()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long pos = mPlayer.getCurrentPosition();
                            mChrono.setText(CloudUtils.makeTimeString(
                                    pos < 3600 * 1000 ? mDurationFormatShort
                                    : mDurationFormatLong, pos / 1000) + " / " +
                                    mCurrentDurationString);
                            mProgressBar.setProgress((int) pos);
                        }
                    });

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }.start();
    }

    private void stopPlayback() {
        try{
            mPlayer.stop();
        } catch (IllegalStateException e){
            Log.e(TAG,"error " + e.toString());
        }
        mProgressBar.setProgress(0);
    }



    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) return;
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                mPlayer.seekTo(progress);
            }
        }
        public void onStopTrackingTouch(SeekBar bar) { }
    };


    void startUpload() {
        if (mCreateService == null) return;

        stopPlayback();

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
            data.put(CloudAPI.Params.DOWNLOADABLE, false);
            data.put(CloudAPI.Params.STREAMABLE, true);


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

            if (mExternalUpload) {
                tags.add("soundcloud:source=android-3rdparty-upload");
            } else {
                tags.add("soundcloud:source=android-record");
            }

            if (mFourSquareVenueId != null) tags.add("foursquare:venue="+mFourSquareVenueId);
            if (mLat  != 0) tags.add("geo:lat="+mLat);
            if (mLong != 0) tags.add("geo:lon="+mLong);
            data.put(CloudAPI.Params.TAG_LIST, TextUtils.join(" ", tags));


            if (mAudioProfile == Profile.RAW && !mExternalUpload) {
                data.put(UploadTask.Params.OGG_FILENAME,new File(mRecordDir, generateFilename(title,"ogg")).getAbsolutePath());
                data.put(UploadTask.Params.ENCODE, true);
                txtUploadingStatus.setText(R.string.record_currently_encoding_uploading);
            } else {
                if (!mExternalUpload){
                    File newRecFile = new File(mRecordDir, generateFilename(title, "mp4"));
                    if (mRecordFile == null || mRecordFile.renameTo(newRecFile)) {
                        mRecordFile = newRecFile;
                    }
                }
                txtUploadingStatus.setText(R.string.record_currently_uploading);
            }

            // WTF
            if (mRecordFile != null) {
                data.put(UploadTask.Params.SOURCE_PATH, mRecordFile.getAbsolutePath());
            }

            if (!TextUtils.isEmpty(mArtworkUri)) {
                data.put(UploadTask.Params.ARTWORK_PATH, mArtworkUri);
            }

            try {
                mCreateService.uploadTrack(data);
            } catch (RemoteException ignored) {
                Log.e(TAG, "error", ignored);
            } finally {
                mRecordFile = null;
            }
        } else {
            showToast(R.string.wait_for_upload_to_finish);
            mCurrentState = CreateState.IDLE_UPLOAD;
            updateUi(true);
        }
    }

    private static String dateString(Time time) {
        String day = DateUtils.getDayOfWeekString(time.weekDay + 1, DateUtils.LENGTH_LONG);
        String dayTime;
        if (time.hour <= 12) {
            dayTime = "morning";
        } else if (time.hour <= 17) {
            dayTime = "afternoon";
        } else if (time.hour <= 21) {
           dayTime = "evening";
        } else {
           dayTime = "night";
        }
        return day + " " + dayTime;
    }



    private String generateFilename(String title, String extension) {
        return String.format("%s_%s.%s", title,
               DateFormat.format("yyyy-MM-dd-hh-mm-ss", mRecordingStarted.toMillis(false)), extension);
    }

    private String generateTitle() {
      return generateSharingNote();
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
                note = String.format("Sounds from %s", mWhereText.getText());
            } else {
                note = String.format("Sounds from %s", dateString(mRecordingStarted));
            }
        }
        return note;
    }

    private void setRecordFile(){
        // find the oldest valid record file in the directory
           for (File f : mRecordDir.listFiles()){
               if ((mRecordFile == null || f.lastModified() < mRecordFile.lastModified()) && (isRawFilename(f.getName()) || isCompressedFilename(f.getName())))
                   mRecordFile = f;
           }

           if (mRecordFile != null) mAudioProfile = isRawFilename(mRecordFile.getName()) ? Profile.RAW : Profile.ENCODED_LOW;
       }

    private boolean isRawFilename(String filename){
        pattern = Pattern.compile(RAW_PATTERN);
        matcher = pattern.matcher(filename);
        return matcher.matches();
    }

    private boolean isCompressedFilename(String filename){
        pattern = Pattern.compile(COMPRESSED_PATTERN);
        matcher = pattern.matcher(filename);
        return matcher.matches();
    }

    public void onRecProgressUpdate(long elapsed) {
        if (elapsed - mLastDisplayedTime > 1000) {
            mChrono.setText(CloudUtils.makeTimeString(
                elapsed < 3600000 ? mDurationFormatShort : mDurationFormatLong, elapsed/1000));
            updateTimeRemaining();
            mLastDisplayedTime = (elapsed / 1000)*1000;
        }
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
                    String[] filePathColumn = { MediaStore.MediaColumns.DATA };
                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    setPickedImage(filePath);
                }
                break;
            case CloudUtils.RequestCodes.GALLERY_IMAGE_TAKE:
                if (resultCode == RESULT_OK) {
                    setTakenImage();
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

}
