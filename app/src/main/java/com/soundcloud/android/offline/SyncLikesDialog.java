package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;

public class SyncLikesDialog extends DialogFragment {

    private static final String TAG = "SyncLikes";

    @Inject OfflineContentOperations offlineOperations;

    public SyncLikesDialog() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    private final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            offlineOperations.setLikesOfflineSync(true);
            dismiss();
        }
    };

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.sync_likes_dialog_title)
                .setMessage(R.string.sync_likes_dialog_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.sync_likes_dialog_accept, listener)
                .create();
    }

}
