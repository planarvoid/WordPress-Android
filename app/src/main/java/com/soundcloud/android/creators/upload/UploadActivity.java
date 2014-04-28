package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.SoundCloudApplication.handleSilentException;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.ButtonBar;
import eu.inmite.android.lib.dialogs.ISimpleDialogListener;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class UploadActivity extends ScActivity implements ISimpleDialogListener {

    private RadioGroup mRdoPrivacy;
    private RadioButton mRdoPrivate, mRdoPublic;
    private ConnectionListLayout mConnectionList;
    private Recording mRecording;
    private RecordingMetaDataLayout mRecordingMetadata;
    private boolean mUploading;

    private ImageOperations mImageOperations;

    private RecordingStorage mStorage;
    private PublicCloudAPI mOldCloudAPI;

    private static final int REC_ANOTHER = 0, POST = 1;

    public static final int DIALOG_PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mOldCloudAPI = new PublicApi(this);
        setTitle(R.string.share);

        mImageOperations = SoundCloudApplication.fromContext(this).getImageOperations();
        mStorage = new RecordingStorage();

        final Intent intent = getIntent();
        if (intent != null && (mRecording = Recording.fromIntent(intent, this, getCurrentUserId())) != null) {
            setUploadLayout(R.layout.sc_upload);
            mRecordingMetadata.setRecording(mRecording, false);

            if (mRecording.external_upload) {
                // 3rd party upload, disable "record another playable button"
                ((ViewGroup) findViewById(R.id.share_user_layout)).addView(
                        new ShareUserHeaderLayout(this, getApp().getLoggedInUser(), mImageOperations));
            }

            if (mRecording.exists()) {
                mapFromRecording(mRecording);
            } else {
                recordingNotFound();
            }
        } else {
            Log.w(TAG, "No recording found in intent, finishing");
            setResult(RESULT_OK, null);
            finish();
        }
    }

    private void setUploadLayout(int layoutId){
        super.setContentView(layoutId);
        mRecordingMetadata = (RecordingMetaDataLayout) findViewById(R.id.metadata_layout);
        mRecordingMetadata.setActivity(this);

        final int backStringId = !mRecording.external_upload ? R.string.record_another_sound :
                mRecording.isLegacyRecording() ? R.string.delete : R.string.cancel;

        ((ButtonBar) findViewById(R.id.bottom_bar)).addItem(new ButtonBar.MenuItem(REC_ANOTHER, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecording.external_upload){
                    mStorage.delete(mRecording);
                } else {
                    setResult(RESULT_OK, new Intent().setData(mRecording.toUri()));
                }
                finish();
            }
        }), backStringId).addItem(new ButtonBar.MenuItem(POST, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecording != null) {
                    saveRecording();
                    mRecording.upload(UploadActivity.this);
                    setResult(RESULT_OK, new Intent().setData(mRecording.toUri()).putExtra(Actions.UPLOAD_EXTRA_UPLOADING, true));
                    mUploading = true;
                    finish();
                } else {
                    recordingNotFound();
                }
            }
        }), R.string.post);

        mRdoPrivacy = (RadioGroup) findViewById(R.id.rdo_privacy);
        mRdoPublic = (RadioButton) findViewById(R.id.rdo_public);
        mRdoPrivate = (RadioButton) findViewById(R.id.rdo_private);

        mRdoPrivacy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rdo_public:
                        mConnectionList.setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.txt_record_options)).setText(R.string.sc_upload_sharing_options_public);
                        break;
                    case R.id.rdo_private:
                        mConnectionList.setVisibility(View.GONE);
                        ((TextView) findViewById(R.id.txt_record_options)).setText(R.string.sc_upload_sharing_options_private);
                        break;
                }
            }
        });

        mConnectionList = (ConnectionListLayout) findViewById(R.id.connectionList);
        mConnectionList.setAdapter(new ConnectionListLayout.Adapter(mOldCloudAPI));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionList.getAdapter().loadIfNecessary(this);
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.RECORD_UPLOAD.get());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mRecording != null && !mUploading && (!mRecording.external_upload)) {
            // recording exists and hasn't been uploaded
            saveRecording();
        }
    }

    private void saveRecording() {
        mapToRecording(mRecording);
        if (mRecording != null) {
            mStorage.store(mRecording);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRecordingMetadata != null) mRecordingMetadata.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt("createPrivacyValue", mRdoPrivacy.getCheckedRadioButtonId());
        mRecordingMetadata.onSaveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (state.getInt("createPrivacyValue") == R.id.rdo_private) {
            mRdoPrivate.setChecked(true);
        } else {
            mRdoPublic.setChecked(true);
        }

        mRecordingMetadata.onRestoreInstanceState(state);
        super.onRestoreInstanceState(state);
    }

    private void mapFromRecording(final Recording recording) {
        mRecordingMetadata.mapFromRecording(recording);
        if (recording.is_private) {
            mRdoPrivate.setChecked(true);
        } else {
            mRdoPublic.setChecked(true);
        }
    }

    private void mapToRecording(final Recording recording) {
        mRecordingMetadata.mapToRecording(recording);
        recording.is_private = mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
        if (!recording.is_private) {
            if (mConnectionList.postToServiceIds() != null) {
                recording.service_ids = TextUtils.join(",", mConnectionList.postToServiceIds());
            }
            recording.shared_emails = null;
        } else {
            recording.service_ids = null;
        }
    }

    private void recordingNotFound() {
        showToast(R.string.recording_not_found);
        finish();
    }

    @Override
    public void onPositiveButtonClicked(int requestCode) {
        switch (requestCode){
            case DIALOG_PICK_IMAGE :
                ImageUtils.startTakeNewPictureIntent(this, mRecording.generateImageFile(Recording.IMAGE_DIR),
                        Consts.RequestCodes.GALLERY_IMAGE_TAKE);
                break;
        }
    }

    @Override
    public void onNegativeButtonClicked(int requestCode) {
        switch (requestCode){
            case DIALOG_PICK_IMAGE :
                ImageUtils.startPickImageIntent(this, Consts.RequestCodes.GALLERY_IMAGE_PICK);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Consts.RequestCodes.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    ImageUtils.sendCropIntent(this, result.getData(), Uri.fromFile(mRecording.generateImageFile(Recording.IMAGE_DIR)));
                }
                break;
            case Consts.RequestCodes.GALLERY_IMAGE_TAKE:
                if (resultCode == RESULT_OK) {
                    ImageUtils.sendCropIntent(this, Uri.fromFile(mRecording.generateImageFile(Recording.IMAGE_DIR)));
                }
                break;

            case Crop.REQUEST_CROP: {
                if (resultCode == RESULT_OK) {
                    mRecordingMetadata.setImage(mRecording.generateImageFile(Recording.IMAGE_DIR));
                } else if (resultCode == Crop.RESULT_ERROR) {
                    handleSilentException("error cropping image", Crop.getError(result));
                    Toast.makeText(this, R.string.crop_image_error, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case Consts.RequestCodes.PICK_VENUE:
                if (resultCode == RESULT_OK && result != null && result.hasExtra(LocationPickerActivity.EXTRA_NAME)) {
                    // XXX candidate for model?
                    mRecordingMetadata.setWhere(result.getStringExtra(LocationPickerActivity.EXTRA_NAME),
                            result.getStringExtra(LocationPickerActivity.EXTRA_4SQ_ID),
                            result.getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, 0),
                            result.getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, 0));
                }
                break;

            case Consts.RequestCodes.MAKE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    boolean success = result.getBooleanExtra("success", false);
                    String msg = getString(
                            success ? R.string.connect_success : R.string.connect_failure,
                            result.getStringExtra("service"));
                    Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();

                    if (success) {
                        // this should reload the services and the list should auto refresh
                        // from the content observer
                        startService(new Intent(this, ApiSyncService.class)
                                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                                        .setData(Content.ME_CONNECTIONS.uri));
                    }
                }
        }
    }



}
