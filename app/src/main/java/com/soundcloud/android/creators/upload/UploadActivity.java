package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordOperations;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.ButtonBar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

public class UploadActivity extends ScActivity {

    private static final int REC_ANOTHER = 0, POST = 1;

    private RadioGroup rdoPrivacy;
    private RadioButton rdoPrivate, rdoPublic;
    private ConnectionListLayout connectionList;
    private Recording recording;
    private RecordingMetaDataLayout recordingMetadata;
    private boolean uploading;
    private RecordingStorage storage;
    private PublicCloudAPI oldCloudAPI;

    @Inject ImageOperations imageOperations;
    @Inject RecordOperations recordOperations;
    @Inject @LightCycle ActionBarController actionBarController;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        oldCloudAPI = new PublicApi(this);
        setTitle(R.string.share);

        storage = new RecordingStorage();

        final Intent intent = getIntent();
        if (intent != null && (recording = Recording.fromIntent(intent, this, accountOperations.getLoggedInUserUrn().getNumericId())) != null) {
            setUploadLayout(R.layout.sc_upload);
            recordingMetadata.setRecording(recording, false);

            if (recording.external_upload) {
                // 3rd party upload, disable "record another playable button"
                ((ViewGroup) findViewById(R.id.share_user_layout)).addView(
                        new ShareUserHeaderLayout(this, accountOperations.getLoggedInUser(), imageOperations));
            }

            if (recording.exists()) {
                mapFromRecording(recording);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void setContentView() {
        super.setContentView(R.layout.sc_upload);
        presenter.setToolBar();
    }

    private void setUploadLayout(int layoutId) {

        recordingMetadata = (RecordingMetaDataLayout) findViewById(R.id.metadata_layout);
        recordingMetadata.setActivity(this);

        final int backStringId = !recording.external_upload ? R.string.record_another_sound :
                recording.isLegacyRecording() ? R.string.delete : R.string.cancel;

        ((ButtonBar) findViewById(R.id.bottom_bar)).addItem(new ButtonBar.MenuItem(REC_ANOTHER, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording.external_upload) {
                    storage.delete(recording);
                } else {
                    setResult(RESULT_OK, new Intent().setData(recording.toUri()));
                }
                finish();
            }
        }), backStringId).addItem(new ButtonBar.MenuItem(POST, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording != null) {
                    saveRecording();
                    recordOperations.upload(UploadActivity.this, recording);
                    setResult(RESULT_OK, new Intent().setData(recording.toUri()).putExtra(Actions.UPLOAD_EXTRA_UPLOADING, true));
                    uploading = true;
                    finish();
                } else {
                    recordingNotFound();
                }
            }
        }), R.string.post);

        rdoPrivacy = (RadioGroup) findViewById(R.id.rdo_privacy);
        rdoPublic = (RadioButton) findViewById(R.id.rdo_public);
        rdoPrivate = (RadioButton) findViewById(R.id.rdo_private);

        rdoPrivacy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rdo_public:
                        connectionList.setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.txt_record_options)).setText(R.string.sc_upload_sharing_options_public);
                        break;
                    case R.id.rdo_private:
                        connectionList.setVisibility(View.GONE);
                        ((TextView) findViewById(R.id.txt_record_options)).setText(R.string.sc_upload_sharing_options_private);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown checkedId: " + checkedId);
                }
            }
        });

        connectionList = (ConnectionListLayout) findViewById(R.id.connectionList);
        connectionList.setAdapter(new ConnectionListLayout.Adapter(oldCloudAPI));
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionList.getAdapter().loadIfNecessary(this);
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.RECORD_UPLOAD));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (recording != null && !uploading && (!recording.external_upload)) {
            // recording exists and hasn't been uploaded
            saveRecording();
        }
    }

    private void saveRecording() {
        mapToRecording(recording);
        if (recording != null) {
            storage.store(recording);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recordingMetadata != null) {
            recordingMetadata.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt("createPrivacyValue", rdoPrivacy.getCheckedRadioButtonId());
        recordingMetadata.onSaveInstanceState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        if (state.getInt("createPrivacyValue") == R.id.rdo_private) {
            rdoPrivate.setChecked(true);
        } else {
            rdoPublic.setChecked(true);
        }

        recordingMetadata.onRestoreInstanceState(state);
        super.onRestoreInstanceState(state);
    }

    private void mapFromRecording(final Recording recording) {
        recordingMetadata.mapFromRecording(recording);
        if (recording.is_private) {
            rdoPrivate.setChecked(true);
        } else {
            rdoPublic.setChecked(true);
        }
    }

    private void mapToRecording(final Recording recording) {
        recordingMetadata.mapToRecording(recording);
        recording.is_private = rdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
        if (!recording.is_private) {
            if (connectionList.postToServiceIds() != null) {
                recording.service_ids = TextUtils.join(",", connectionList.postToServiceIds());
            }
            recording.shared_emails = null;
        } else {
            recording.service_ids = null;
        }
    }

    private void recordingNotFound() {
        AndroidUtils.showToast(this, R.string.recording_not_found);
        finish();
    }

    @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Consts.RequestCodes.GALLERY_IMAGE_PICK:
                if (resultCode == RESULT_OK) {
                    ImageUtils.sendCropIntent(this, result.getData(), Uri.fromFile(recording.generateImageFile(Recording.IMAGE_DIR)));
                }
                break;
            case Consts.RequestCodes.GALLERY_IMAGE_TAKE:
                if (resultCode == RESULT_OK) {
                    ImageUtils.sendCropIntent(this, Uri.fromFile(recording.generateImageFile(Recording.IMAGE_DIR)));
                }
                break;

            case Crop.REQUEST_CROP: {
                if (resultCode == RESULT_OK) {
                    recordingMetadata.setImage(recording.generateImageFile(Recording.IMAGE_DIR));
                } else if (resultCode == Crop.RESULT_ERROR) {
                    ErrorUtils.handleSilentException("error cropping image", Crop.getError(result));
                    Toast.makeText(this, R.string.crop_image_error, Toast.LENGTH_SHORT).show();
                }
                break;
            }

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
                break;
            default:
                throw new IllegalArgumentException("Unknown requestCode: " + requestCode);
        }
    }


}
