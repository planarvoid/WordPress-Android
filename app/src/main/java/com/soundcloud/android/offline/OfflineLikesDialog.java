package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;

public class OfflineLikesDialog extends DialogFragment {

    private static final String TAG = "OfflineLikes";

    @Inject OfflineContentOperations offlineOperations;

    private final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            offlineOperations.setOfflineLikesEnabled(true);
            dismiss();
        }
    };

    public OfflineLikesDialog() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.offline_likes_dialog_title)
                .setMessage(R.string.offline_likes_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.make_offline_available, listener)
                .create();
    }

}
