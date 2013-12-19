package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.SoundCloudApplication.handleSilentException;

import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.ButtonBar;
import eu.inmite.android.lib.dialogs.ISimpleDialogListener;

import android.content.Intent;
import android.database.DataSetObserver;
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
import android.widget.ViewFlipper;

import java.util.Arrays;
import java.util.List;

public class UploadActivity extends ScActivity implements ISimpleDialogListener {

    private ViewFlipper mSharingFlipper;
    private RadioGroup mRdoPrivacy;
    private RadioButton mRdoPrivate, mRdoPublic;
    private ConnectionListLayout mConnectionList;
    private AccessListLayout mAccessListLayout;
    private Recording mRecording;
    private RecordingMetaDataLayout mRecordingMetadata;
    private boolean mUploading;

    private ImageOperations mImageOperations = ImageOperations.newInstance();

    private RecordingStorage mStorage;
    private PublicCloudAPI mOldCloudAPI;

    private static final int REC_ANOTHER = 0, POST = 1;

    public static final int DIALOG_PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mOldCloudAPI = new PublicApi(this);
        setTitle(R.string.share);

        mStorage = new RecordingStorage();

        final Intent intent = getIntent();
        if (intent != null && (mRecording = Recording.fromIntent(intent, this, getCurrentUserId())) != null) {
            setUploadLayout(mRecording.isPrivateMessage() ? R.layout.sc_message_upload : R.layout.sc_upload);
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
                mRecording.isLegacyRecording() ? R.string.delete :
                        R.string.cancel;

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
        }), mRecording.isPrivateMessage() ? R.string.private_message_btn_send : R.string.post);

        if (mRecording.isPrivateMessage()) {
            ((TextView) findViewById(R.id.txt_private_message_upload_message))
                    .setText(getString(R.string.private_message_upload_message, mRecording.getRecipientUsername()));
        } else {
            mSharingFlipper = (ViewFlipper) findViewById(R.id.vfSharing);
            mRdoPrivacy = (RadioGroup) findViewById(R.id.rdo_privacy);
            mRdoPublic = (RadioButton) findViewById(R.id.rdo_public);
            mRdoPrivate = (RadioButton) findViewById(R.id.rdo_private);

            mRdoPrivacy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.rdo_public:
                            mSharingFlipper.setDisplayedChild(0);
                            ((TextView) findViewById(R.id.txt_record_options)).setText(R.string.sc_upload_sharing_options_public);
                            break;
                        case R.id.rdo_private:
                            mSharingFlipper.setDisplayedChild(1);
                            ((TextView) findViewById(R.id.txt_record_options)).setText(R.string.sc_upload_sharing_options_private);
                            break;
                    }
                }
            });

            mConnectionList = (ConnectionListLayout) findViewById(R.id.connectionList);
            mConnectionList.setAdapter(new ConnectionListLayout.Adapter(mOldCloudAPI));

            mAccessListLayout = (AccessListLayout) findViewById(R.id.accessList);
            mAccessListLayout.registerDataSetObserver(new DataSetObserver() {
                @Override public void onChanged() {
                    findViewById(R.id.btn_add_emails).setVisibility(
                            mAccessListLayout.isEmpty() ? View.VISIBLE : View.GONE
                    );
                }
            });

            findViewById(R.id.btn_add_emails).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<String> accessList = mAccessListLayout.get();
                    Intent intent = new Intent(UploadActivity.this, EmailPickerActivity.class);
                    if (accessList != null) {
                        intent.putExtra(EmailPickerActivity.BUNDLE_KEY, accessList.toArray(new String[accessList.size()]));
                        if (v instanceof EmailPickerItemLayout) {
                            intent.putExtra(EmailPickerActivity.SELECTED, ((EmailPickerItemLayout) v).getEmail());
                        }
                    }
                    startActivityForResult(intent, Consts.RequestCodes.PICK_EMAILS);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mRecording.isPrivateMessage()) {
            mConnectionList.getAdapter().loadIfNecessary(this);
        }
        if (shouldTrackScreen()) {
            Event.SCREEN_ENTERED.publish(Screen.RECORD_UPLOAD.get());
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

    private void setPrivateShareEmails(String[] emails) {
        mAccessListLayout.set(Arrays.asList(emails));
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        if (!mRecording.isPrivateMessage()) {
            state.putInt("createPrivacyValue", mRdoPrivacy.getCheckedRadioButtonId());
        }
        mRecordingMetadata.onSaveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (!mRecording.isPrivateMessage()) {
            if (state.getInt("createPrivacyValue") == R.id.rdo_private) {
                mRdoPrivate.setChecked(true);
            } else {
                mRdoPublic.setChecked(true);
            }
        }

        mRecordingMetadata.onRestoreInstanceState(state);
        super.onRestoreInstanceState(state);
    }

    private void mapFromRecording(final Recording recording) {
        mRecordingMetadata.mapFromRecording(recording);
        if (!mRecording.isPrivateMessage()) {
            if (!TextUtils.isEmpty(recording.shared_emails)) setPrivateShareEmails(recording.shared_emails.split(","));
            if (recording.is_private) {
                mRdoPrivate.setChecked(true);
            } else {
                mRdoPublic.setChecked(true);
            }
        }
    }

    private void mapToRecording(final Recording recording) {
        mRecordingMetadata.mapToRecording(recording);
        if (!mRecording.isPrivateMessage()) {
            recording.is_private = mRdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
            if (!recording.is_private) {
                if (mConnectionList.postToServiceIds() != null) {
                    recording.service_ids = TextUtils.join(",", mConnectionList.postToServiceIds());
                }
                recording.shared_emails = null;
            } else {
                recording.service_ids = null;
                if (mAccessListLayout.get() != null) {
                    recording.shared_emails = TextUtils.join(",", mAccessListLayout.get());
                }
            }
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

            case Consts.RequestCodes.IMAGE_CROP: {
                if (resultCode == RESULT_OK) {
                    if (result.getExtras().containsKey("error")) {
                        handleSilentException("error cropping image", (Exception) result.getSerializableExtra("error"));
                        Toast.makeText(this,R.string.crop_image_error, Toast.LENGTH_SHORT).show();
                    } else {
                        mRecordingMetadata.setImage(mRecording.generateImageFile(Recording.IMAGE_DIR));
                    }
                }
                break;
            }

            case Consts.RequestCodes.PICK_EMAILS:
                if (resultCode == RESULT_OK && result != null && result.hasExtra(EmailPickerActivity.BUNDLE_KEY)) {
                    String[] emails = result.getExtras().getStringArray(EmailPickerActivity.BUNDLE_KEY);
                    if (emails != null) {
                        setPrivateShareEmails(emails);
                    }
                }
                break;
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
