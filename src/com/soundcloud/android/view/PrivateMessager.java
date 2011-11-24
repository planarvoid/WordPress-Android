package com.soundcloud.android.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LocationPicker;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScUpload;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

public class PrivateMessager extends ScTabView implements CreateController.CreateListener{

    private SafeViewFlipper mViewFlipper;
    private User mUser;
    private Recording mRecording;

    private CreateController mCreateController;
    private RecordingMetaData mRecordingMetadata;

    public PrivateMessager(ScActivity activity, User user) {
        super(activity);

        mUser = user;
        mRecording = Recording.pendingFromPrivateUserId(mUser.id,mActivity.getContentResolver());

        mViewFlipper = new SafeViewFlipper(activity);
        addView(mViewFlipper,new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));

        View v = new View(activity);
        v.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.drop_shadow_down));
        addView(v,new LayoutParams(LayoutParams.FILL_PARENT, (int) (5*activity.getResources().getDisplayMetrics().density)));

        ViewGroup createLayout = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.sc_create, null);
        mCreateController = new CreateController(activity, createLayout, mRecording, mUser);
        mCreateController.setInstructionsText(activity.getString(R.string.private_message_title, mUser.username));
        mCreateController.setListener(this);
        mViewFlipper.addView(createLayout);

        ViewGroup uploadLayout = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.sc_message_upload, null);
        mRecordingMetadata = (RecordingMetaData) uploadLayout.findViewById(R.id.metadata_layout);
        mRecordingMetadata.setActivity(activity);
        mViewFlipper.addView(uploadLayout);

        ((TextView) uploadLayout.findViewById(R.id.txt_private_message_upload_message))
                .setText(activity.getString(R.string.private_message_upload_message, mUser.getDisplayName()));

        uploadLayout.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // reset
                mapToRecording(mRecording);
                saveRecording(mRecording);
                flipToCreate();
            }
        });

        uploadLayout.findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecording != null) {
                    mapToRecording(mRecording);
                    mActivity.trackEvent(Consts.Tracking.Categories.AUDIO_MESSAGE, "send");
                    saveRecording(mRecording);
                    mActivity.startUpload(mRecording);
                    mRecording = null;
                    mRecordingMetadata.reset();
                    mCreateController.reset();
                    flipToCreate();

                    // TODO no creating more messages until current done uploading
                }
            }
        });
    }

    private void flipToCreate(){
        mViewFlipper.setInAnimation(AnimUtils.inFromTopAnimation(500));
        mViewFlipper.setOutAnimation(AnimUtils.outToBottomAnimation(500));
        mViewFlipper.showPrevious();
    }

    public void onResume() {
        mCreateController.onResume();
    }

    public void onPause() {
        mCreateController.onPause();
    }

    public void onStart() {
        mCreateController.onStart();
    }

    public void onStop() {
        mCreateController.onStop();
        if (mRecording != null) {
            // recording exists and hasn't been uploaded
            mapToRecording(mRecording);
            saveRecording(mRecording);
        }
    }

    private void saveRecording(Recording r) {
        if (r != null && !r.external_upload) {
            mActivity.getContentResolver().update(r.toUri(), r.buildContentValues(), null, null);
        }
    }

    public void onDestroy() {
        mCreateController.onDestroy();
        mRecordingMetadata.onDestroy();
    }

    public void onSaveInstanceState(Bundle state) {
        state.putInt("privateMessagerCurrentIndex", mViewFlipper.indexOfChild(mViewFlipper.getCurrentView()));
        mCreateController.onSaveInstanceState(state);
        mRecordingMetadata.onSaveInstanceState(state);
    }

    public void onRestoreInstanceState(Bundle state){
        mViewFlipper.setDisplayedChild(state.getInt("privateMessagerCurrentIndex"));
        mCreateController.onRestoreInstanceState(state);
        mRecordingMetadata.onRestoreInstanceState(state);
    }

    public void onCreateServiceBound(ICloudCreateService mCreateService) {
        mCreateController.onCreateServiceBound(mCreateService);
    }

    public Dialog onCreateDialog(int which) {
        return mCreateController.onCreateDialog(which);
    }

    private void mapFromRecording(final Recording recording) {
        mRecordingMetadata.mapFromRecording(recording);
    }

    private void mapToRecording(final Recording recording) {
        mRecordingMetadata.mapToRecording(recording);
    }

    @Override
    public void onSave(Uri recordingUri, Recording recording, boolean newRecording) {
        mRecording = recording;
        mRecordingMetadata.setRecording(mRecording, true);
        mViewFlipper.setInAnimation(AnimUtils.inFromBottomAnimation(500));
        mViewFlipper.setOutAnimation(AnimUtils.outToTopAnimation(500));
        mViewFlipper.showNext();

        mCreateController.updateUi(false);
    }

    @Override
    public void onCancel() {
        mCreateController.reset();
    }

    @Override
    public void onDelete() {
        mCreateController.reset();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case ImageUtils.ImagePickListener.GALLERY_IMAGE_PICK:
                if (resultCode == Activity.RESULT_OK) {
                    mRecordingMetadata.setImage(CloudUtils.getFromMediaUri(mActivity.getContentResolver(), result.getData()));
                }
                break;
            case ImageUtils.ImagePickListener.GALLERY_IMAGE_TAKE:
                if (resultCode == Activity.RESULT_OK) {
                mRecordingMetadata.setDefaultImage();
                }
                break;
            case LocationPicker.PICK_VENUE:
                if (resultCode == Activity.RESULT_OK && result != null && result.hasExtra("name")) {
                    // XXX candidate for model?
                mRecordingMetadata.    setWhere(result.getStringExtra("name"),
                            result.getStringExtra("id"),
                            result.getDoubleExtra("longitude", 0),
                            result.getDoubleExtra("latitude", 0));
                }
                break;
        }
    }

    public void setRecording(Uri recordingUri) {
        mRecording = Recording.fromUri(recordingUri, mActivity.getContentResolver());
        if (mRecording != null){
            mCreateController.setRecording(mRecording);
            mRecordingMetadata.setRecording(mRecording, true);
        }
    }
}
