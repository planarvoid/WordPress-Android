package com.soundcloud.android.activity;


import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.tracking.Click;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracking;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.AccessList;
import com.soundcloud.android.view.ConnectionList;
import com.soundcloud.android.view.create.RecordingMetaData;
import com.soundcloud.android.view.create.ShareUserHeader;

import android.content.Intent;
import android.database.DataSetObserver;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Tracking(page = Page.Record_details)
public class ScUpload extends ScActivity {
    private ViewFlipper mSharingFlipper;
    private RadioGroup mRdoPrivacy;
    /* package */ RadioButton mRdoPrivate, mRdoPublic;
    /* package */ ConnectionList mConnectionList;
    /* package */ AccessList mAccessList;
    private Recording mRecording;
    private RecordingMetaData mRecordingMetadata;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.sc_upload);
        initResourceRefs();

        final Intent intent = getIntent();
        if (intent != null && (mRecording = Recording.fromIntent(intent,getContentResolver(),getCurrentUserId())) != null) {
            mRecordingMetadata.setRecording(mRecording);
            if (mRecording.external_upload) {
                // 3rd party upload, disable "record another sound button"
                findViewById(R.id.btn_cancel).setVisibility(View.GONE);
                ((ViewGroup) findViewById(R.id.share_user_layout)).addView(
                    new ShareUserHeader(this, getApp().getLoggedInUser()));
            }

            if (mRecording.exists()) {
                mapFromRecording(mRecording);
            } else {
                errorOut(R.string.recording_not_found);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mConnectionList.getAdapter().loadIfNecessary();
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
        mAccessList.getAdapter().setAccessList(Arrays.asList(emails));
    }

    private void initResourceRefs() {
        mRecordingMetadata = (RecordingMetaData) findViewById(R.id.metadata_layout);
        mRecordingMetadata.setActivity(this);

        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_details_record_another);
                startActivity((new Intent(Actions.RECORD))
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
            }
        });

        findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                track(Click.Record_details_Upload_and_share);

                if (mRecording != null) {
                    mapToRecording(mRecording);
                    saveRecording(mRecording);
                }

                if (startUpload()) {
                    mRecording = null;
                    mRecordingMetadata.setRecording(null);
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });

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
                Intent intent = new Intent(ScUpload.this, EmailPicker.class);
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

    /* package */ boolean startUpload() {
        return mRecording != null && startUpload(mRecording);
    }

    private void errorOut(int error) {
        showToast(error);
        finish();
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

    // for testing purposes
    void setRecording(Recording r) {
        mRecording = r;
        mRecordingMetadata.setRecording(mRecording);
        mapToRecording(r);
    }

    private void mapFromRecording(final Recording recording) {
        mRecordingMetadata.mapFromRecording(recording);
        if (!TextUtils.isEmpty(recording.shared_emails)) setPrivateShareEmails(recording.shared_emails.split(","));
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
            if (mAccessList.getAdapter().getAccessList() != null) {
                recording.shared_emails = TextUtils.join(",", mAccessList.getAdapter().getAccessList());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case ImageUtils.ImagePickListener.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    mRecordingMetadata.setImage(IOUtils.getFromMediaUri(getContentResolver(), result.getData()));
                }
                break;
            case ImageUtils.ImagePickListener.GALLERY_IMAGE_TAKE:
                if (resultCode == RESULT_OK) {
                mRecordingMetadata.setDefaultImage();
                }
                break;

            case EmailPicker.PICK_EMAILS:
                if (resultCode == RESULT_OK && result != null && result.hasExtra(EmailPicker.BUNDLE_KEY)) {
                    String[] emails = result.getExtras().getStringArray(EmailPicker.BUNDLE_KEY);
                    if (emails != null) {
                        setPrivateShareEmails(emails);
                    }
                }
                break;
            case LocationPicker.PICK_VENUE:
                if (resultCode == RESULT_OK && result != null && result.hasExtra("name")) {
                    // XXX candidate for model?
                mRecordingMetadata.    setWhere(result.getStringExtra("name"),
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

                    if (success) {
                        mConnectionList.getAdapter().load();
                    }
                }
        }
    }
}
