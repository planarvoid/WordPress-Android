package com.soundcloud.android.view;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScUpload;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.utils.AnimUtils;
import org.apache.commons.logging.Log;

public class PrivateMessager extends ScTabView implements CreateController.CreateListener{

    private ViewFlipper mViewFlipper;
    private User mUser;

    private CreateController mCreateController;
    private RecordingMetaData mRecordingMetaData;

    public PrivateMessager(ScActivity activity, User user) {
        super(activity);

        mUser = user;

        mViewFlipper = new ViewFlipper(activity);
        addView(mViewFlipper,new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));

        ViewGroup createLayout = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.sc_create, null);
        mCreateController = new CreateController(activity, createLayout, null, mUser);
        mCreateController.setInstructionsText(activity.getString(R.string.private_message_title));
        mCreateController.setListener(this);
        mViewFlipper.addView(createLayout);


        ViewGroup uploadLayout = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.sc_message_upload, null);
        mRecordingMetaData = (RecordingMetaData) uploadLayout.findViewById(R.id.metadata_layout);
        mRecordingMetaData.setActivity(activity);
        mViewFlipper.addView(uploadLayout);

        ((TextView) uploadLayout.findViewById(R.id.txt_private_message_upload_message)).setText(activity.getString(R.string.private_message_upload_message,mUser.username));

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
    }

    public void onDestroy() {
        mCreateController.onDestroy();
    }

    public void onSaveInstanceState(Bundle state) {
        state.putInt("privateMessagerCurrentIndex",mViewFlipper.indexOfChild(mViewFlipper.getCurrentView()));
        mCreateController.onSaveInstanceState(state);
    }

    public void onRestoreInstanceState(Bundle state){
        mViewFlipper.setDisplayedChild(state.getInt("privateMessagerCurrentIndex"));
    }

    public void onCreateServiceBound(ICloudCreateService mCreateService) {
        mCreateController.onCreateServiceBound(mCreateService);
    }

    public Dialog onCreateDialog(int which) {
        return mCreateController.onCreateDialog(which);
    }

    @Override
    public void onSave(Uri recording) {
        mViewFlipper.setInAnimation(AnimUtils.inFromBottomAnimation());
        mViewFlipper.setOutAnimation(AnimUtils.outToTopAnimation());
        mViewFlipper.showNext();
    }

    @Override
    public void onCancel() {
        // ignore
    }
}
