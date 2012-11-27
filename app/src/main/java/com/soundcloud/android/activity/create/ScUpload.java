package com.soundcloud.android.activity.create;


import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.ButtonBar;
import com.soundcloud.android.view.create.AccessList;
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
    private boolean mUploading;


    private static final int REC_ANOTHER = 0, POST = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setTitle(R.string.share);

        final Intent intent = getIntent();
        if (intent != null && (mRecording = Recording.fromIntent(intent, getContentResolver(), getCurrentUserId())) != null) {
            setContentView(mRecording.isPrivateMessage() ? R.layout.sc_message_upload : R.layout.sc_upload);
            mRecordingMetadata.setRecording(mRecording, false);

            if (mRecording.external_upload) {
                // 3rd party upload, disable "record another sound button"
                ((ViewGroup) findViewById(R.id.share_user_layout)).addView(
                        new ShareUserHeader(this, getApp().getLoggedInUser()));
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

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    @Override
    public void setContentView(int layoutId) {
        super.setContentView(layoutId);
        mRecordingMetadata = (RecordingMetaData) findViewById(R.id.metadata_layout);
        mRecordingMetadata.setActivity(this);

        final int backStringId = !mRecording.external_upload ? R.string.record_another_sound :
                                 mRecording.isLegacyRecording() ? R.string.delete :
                                 R.string.cancel;

        ((ButtonBar) findViewById(R.id.bottom_bar)).addItem(new ButtonBar.MenuItem(REC_ANOTHER, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_Share_Record_Another);

                if (mRecording.external_upload){
                    mRecording.delete(getContentResolver());
                } else {
                    setResult(RESULT_OK, new Intent().setData(mRecording.toUri()));
                }
                finish();
            }
        }), backStringId).addItem(new ButtonBar.MenuItem(POST, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStream ps = mRecording.getPlaybackStream();
                track(Click.Record_Share_Post,
                      mRecording.tip_key == null ? "tip_unknown" : mRecording.tip_key,
                      ps != null && ps.isTrimmed() ? "trimmed" : "not_trimmed",
                      ps != null && ps.isFading()  ? "fading"  : "not_fading");
                if (mRecording != null) {
                    saveRecording();
                    mRecording.upload(ScUpload.this);
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

        if (mRecording != null && !mUploading && (!mRecording.external_upload)) {
            // recording exists and hasn't been uploaded
            saveRecording();
        }
    }

    private void saveRecording() {
        mapToRecording(mRecording);
        if (mRecording != null) {
            SoundCloudDB.upsertRecording(getContentResolver(), mRecording, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mRecordingMetadata != null) mRecordingMetadata.onDestroy();
    }

    private void setPrivateShareEmails(String[] emails) {
        mAccessList.set(Arrays.asList(emails));
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

    private void recordingNotFound() {
        showToast(R.string.recording_not_found);
        finish();
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
