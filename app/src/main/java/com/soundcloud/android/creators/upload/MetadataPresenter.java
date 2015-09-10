package com.soundcloud.android.creators.upload;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.image.PlaceholderGenerator;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ViewHelper;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.lightcycle.SupportFragmentLightCycleDispatcher;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

public class MetadataPresenter extends SupportFragmentLightCycleDispatcher<Fragment> {
    public static final String RECORDING_KEY = "recording";

    private Recording recording;
    private MetadataFragment metadataFragment;
    private SoundRecorder recorder;
    private final ViewHelper viewHelper;
    private PlaceholderGenerator placeholderGenerator;

    @Bind(R.id.rdo_privacy) RadioGroup rdoPrivacy;
    @Bind(R.id.rdo_private) RadioButton rdoPrivate;
    @Bind(R.id.rdo_public) RadioButton rdoPublic;
    @Bind(R.id.txt_record_options) TextView txtRecordOptions;
    @Bind(R.id.metadata_layout) RecordingMetaDataLayout recordingMetadata;
    @Bind(R.id.btn_action) ImageButton actionButton;

    @Inject
    public MetadataPresenter(SoundRecorder recorder, PlaceholderGenerator placeholderGenerator, ViewHelper viewHelper) {
        this.recorder = recorder;
        this.placeholderGenerator = placeholderGenerator;
        this.viewHelper = viewHelper;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle state) {
        super.onCreate(fragment, state);
        this.metadataFragment = (MetadataFragment) fragment;
        metadataFragment.getActivity().setTitle(R.string.post);
    }

    @Override
    public void onStart(Fragment fragment) {
        super.onStart(fragment);
        this.metadataFragment = (MetadataFragment) fragment;
    }

    @Override
    public void onStop(Fragment fragment) {
        super.onStop(fragment);

        if (recording != null && (!recording.external_upload)) {
            // recording exists and hasn't been uploaded
            mapToRecording(recording);
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        super.onResume(fragment);
        restoreRecording();
        ((RecordActivity) metadataFragment.getActivity())
                .trackScreen(ScreenEvent.create(Screen.RECORD_UPLOAD));
    }

    @Override
    public void onPause(Fragment fragment) {
        recordingMetadata.mapToRecording(recording);
        super.onPause(fragment);
    }

    private void restoreRecording() {
        this.recording = recorder.getRecording();

        if (this.recording != null) {
            if (!recordingMetadata.hasPlaceholder()) {
                recordingMetadata.setPlaceholder(placeholderGenerator.generateDrawable(String.valueOf(recording.hashCode())));
            }
            recordingMetadata.setRecording(recording, true);
        } else {
            ((RecordActivity) metadataFragment.getActivity()).displayRecord();
        }
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.bind(this, view);

        final int orangeButtonDimension = view.getResources().getDimensionPixelSize(R.dimen.rec_upload_button_dimension);
        viewHelper.setCircularButtonOutline(actionButton, orangeButtonDimension);
        actionButton.setImageResource(R.drawable.ic_record_upload_orange);

        if (savedInstanceState != null) {
            if (savedInstanceState.getInt("createPrivacyValue") == R.id.rdo_private) {
                rdoPrivate.setChecked(true);
            } else {
                rdoPublic.setChecked(true);
            }
            recordingMetadata.onRestoreInstanceState(savedInstanceState);
        }

        recordingMetadata.setActivity(metadataFragment.getActivity());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        super.onDestroyView(fragment);

        if (recordingMetadata != null) {
            recordingMetadata.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle state) {
        state.putInt("createPrivacyValue", rdoPrivacy.getCheckedRadioButtonId());
        recordingMetadata.onSaveInstanceState(state);
        super.onSaveInstanceState(fragment, state);
    }

    public void onArtworkSelected(Intent imageSelectionResult) {
        final Uri artworkFileUri = Uri.fromFile(recording.getImageFile());
        if (imageSelectionResult != null) {
            ImageUtils.sendCropIntent(metadataFragment.getActivity(), imageSelectionResult.getData(), artworkFileUri);
        } else {
            // we supplied the artworkFileUri
            ImageUtils.sendCropIntent(metadataFragment.getActivity(), artworkFileUri);
        }
    }

    public void onSuccessfulCrop() {
        recording.artwork_path = recording.getImageFile();
        recordingMetadata.setImage(recording.artwork_path);
    }

    private void mapToRecording(final Recording recording) {
        recordingMetadata.mapToRecording(recording);
        recording.is_private = rdoPrivacy.getCheckedRadioButtonId() == R.id.rdo_private;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Consts.RequestCodes.GALLERY_IMAGE_PICK:
            case Consts.RequestCodes.GALLERY_IMAGE_TAKE:
                if (resultCode == Activity.RESULT_OK) {
                    onArtworkSelected(result);
                }
                break;

            case Crop.REQUEST_CROP: {
                if (resultCode == Activity.RESULT_OK) {
                    onSuccessfulCrop();
                } else if (resultCode == Crop.RESULT_ERROR) {
                    ErrorUtils.handleSilentException("error cropping image", Crop.getError(result));
                    Toast.makeText(metadataFragment.getActivity(), R.string.crop_image_error, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown requestCode: " + requestCode);
        }
    }

    @OnClick(R.id.btn_action)
    void onPost() {
        if (recording != null) {
            RecordActivity activity = (RecordActivity) metadataFragment.getActivity();
            mapToRecording(recording);
            activity.startUpload(recording);
            activity.onMonitorToUpload(recording);
        }
    }
}
