package com.soundcloud.android.activity.create;


import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.create.AccessList;
import com.soundcloud.android.view.ButtonBar;
import com.soundcloud.android.view.create.ConnectionList;
import com.soundcloud.android.view.create.EmailPickerItem;
import com.soundcloud.android.view.create.RecordingMetaData;
import com.soundcloud.android.view.create.ShareUserHeader;

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

@Tracking(page = Page.Record_details)
public class ScUpload extends ScActivity {

    private ViewFlipper mSharingFlipper;
    private RadioGroup mRdoPrivacy;
    private RadioButton mRdoPrivate, mRdoPublic;
    private ConnectionList mConnectionList;
    private AccessList mAccessList;
    private Recording mRecording;
    private RecordingMetaData mRecordingMetadata;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        final Intent intent = getIntent();
        if (intent != null && (mRecording = Recording.fromIntent(intent, getContentResolver(), getCurrentUserId())) != null) {
            setContentView(mRecording.isPrivateMessage() ? R.layout.sc_message_upload : R.layout.sc_upload);

            mRecordingMetadata.setRecording(mRecording, false);
            if (mRecording.external_upload) {
                // 3rd party upload, disable "record another sound button"
                // TODO, this needs to be fixed, there is no cancel button on this screen
                // findViewById(R.id.btn_cancel).setVisibility(View.GONE);
                ((ViewGroup) findViewById(R.id.share_user_layout)).addView(
                        new ShareUserHeader(this, getApp().getLoggedInUser()));
            }

            if (mRecording.exists()) {
                mapFromRecording(mRecording);
            } else {
                errorOut(R.string.recording_not_found);
            }
        } else {
            Log.e(getClass().getSimpleName(), "No recording found in intent, finishing");
            finish();
        }
    }

    @Override
    public void setContentView(int layoutId) {
        super.setContentView(layoutId);
        mRecordingMetadata = (RecordingMetaData) findViewById(R.id.metadata_layout);
        mRecordingMetadata.setActivity(this);

        ((ButtonBar) findViewById(R.id.bottom_bar)).addItem(new ButtonBar.MenuItem(0, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_details_record_another);
                setResult(RESULT_OK, new Intent().setData(mRecording.toUri()));
                finish();
            }
        }), R.string.record_another_sound).addItem(new ButtonBar.MenuItem(1, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_details_Upload_and_share);
                if (mRecording != null) {
                    mapToRecording(mRecording);
                    saveRecording(mRecording);
                    mRecording.upload(ScUpload.this);
                    setResult(RESULT_OK, new Intent().setData(mRecording.toUri()).putExtra(Actions.UPLOAD_EXTRA_UPLOADING,true));
                    finish();
                } else {
                    errorOut(R.string.recording_not_found);
                }
            }
        }), mRecording.isPrivateMessage() ? R.string.private_message_btn_send : R.string.upload_and_share);

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

            mConnectionList = (ConnectionList) findViewById(R.id.connectionList);
            mConnectionList.setAdapter(new ConnectionList.Adapter(this.getApp()));

            mAccessList = (AccessList) findViewById(R.id.accessList);
            mAccessList.registerDataSetObserver(new DataSetObserver() {
                @Override public void onChanged() {
                    findViewById(R.id.btn_add_emails).setVisibility(
                        mAccessList.isEmpty() ? View.VISIBLE : View.GONE
                    );
                }
            });

            findViewById(R.id.btn_add_emails).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<String> accessList = mAccessList.get();
                    Intent intent = new Intent(ScUpload.this, EmailPicker.class);
                    if (accessList != null) {
                        intent.putExtra(EmailPicker.BUNDLE_KEY, accessList.toArray(new String[accessList.size()]));
                        if (v instanceof EmailPickerItem) {
                            intent.putExtra(EmailPicker.SELECTED, ((EmailPickerItem) v).getEmail());
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
            mConnectionList.getAdapter().loadIfNecessary();
        }
        track(getClass(), getApp().getLoggedInUser());
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mRecording != null) {
            // recording exists and hasn't been uploaded
            mapToRecording(mRecording);
            saveRecording(mRecording);
        }
    }

    private void saveRecording(Recording r) {
        if (r != null && !r.external_upload) {
            getContentResolver().update(r.toUri(), r.buildContentValues(), null, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecordingMetadata.onDestroy();
    }

    private void setPrivateShareEmails(String[] emails) {
        mAccessList.set(Arrays.asList(emails));
    }

    private void errorOut(int error) {
        showToast(error);
        finish();
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
                if (mAccessList.get() != null) {
                    recording.shared_emails = TextUtils.join(",", mAccessList.get());
                }
            }
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
                    mRecordingMetadata.setImage(mRecording.generateImageFile(Recording.IMAGE_DIR));
                }
                break;
            }

            case Consts.RequestCodes.PICK_EMAILS:
                if (resultCode == RESULT_OK && result != null && result.hasExtra(EmailPicker.BUNDLE_KEY)) {
                    String[] emails = result.getExtras().getStringArray(EmailPicker.BUNDLE_KEY);
                    if (emails != null) {
                        setPrivateShareEmails(emails);
                    }
                }
                break;
            case Consts.RequestCodes.PICK_VENUE:
                if (resultCode == RESULT_OK && result != null && result.hasExtra(LocationPicker.EXTRA_NAME)) {
                    // XXX candidate for model?
                    mRecordingMetadata.setWhere(result.getStringExtra(LocationPicker.EXTRA_NAME),
                            result.getStringExtra(LocationPicker.EXTRA_4SQ_ID),
                            result.getDoubleExtra(LocationPicker.EXTRA_LONGITUDE, 0),
                            result.getDoubleExtra(LocationPicker.EXTRA_LATITUDE, 0));
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
                        mConnectionList.getAdapter().load();
                    }
                }
        }
    }



}
