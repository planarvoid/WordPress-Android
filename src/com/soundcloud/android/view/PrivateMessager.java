package com.soundcloud.android.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ViewFlipper;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.service.ICloudCreateService;

public class PrivateMessager extends ScTabView {

    private ViewFlipper mViewFlipper;
    private CreateController mCreateController;
    private User mUser;

    public PrivateMessager(ScActivity activity, User user) {
        super(activity);

        mUser = user;

        mViewFlipper = new ViewFlipper(activity);
        addView(mViewFlipper,new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));

        ViewGroup createLayout = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.sc_create, null);
        mCreateController = new CreateController(activity, createLayout, null, mUser);
        mCreateController.setInstructionsText(activity.getString(R.string.private_message_title));

        mViewFlipper.addView(createLayout);
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
        mCreateController.onSaveInstanceState(state);
    }

    public void onCreateServiceBound(ICloudCreateService mCreateService) {
        mCreateController.onCreateServiceBound(mCreateService);
    }

    public Dialog onCreateDialog(int which) {
        return mCreateController.onCreateDialog(which);
    }
}
