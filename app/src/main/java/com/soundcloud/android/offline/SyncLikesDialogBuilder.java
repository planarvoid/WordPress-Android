package com.soundcloud.android.offline;

import com.soundcloud.android.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.preference.PreferenceActivity;

import javax.inject.Inject;

//TODO: change to fragment dialog after likes fragment update
public class SyncLikesDialogBuilder {

    private final OfflineContentOperations offlineOperations;

    @Inject
    public SyncLikesDialogBuilder(OfflineContentOperations offlineOperations) {
        this.offlineOperations = offlineOperations;
    }

    public AlertDialog.Builder create(PreferenceActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.sync_likes_dialog_title));
        builder.setMessage(activity.getString(R.string.sync_likes_dialog_message));
        builder.setNegativeButton(activity.getString(R.string.cancel), null);
        builder.setPositiveButton(activity.getString(R.string.sync_likes_dialog_accept),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        offlineOperations.setLikesOfflineSync(true);
                    }
                });
        return builder;
    }

}