package com.soundcloud.android.service.upload;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

/**
 * Checks for uploads which are stuck in state {@link com.soundcloud.android.model.Recording.Status.UPLOADING}, for
 * example after a service crash.
 */
public class StuckUploadCheck implements Runnable {
    public StuckUploadCheck(Context context) {
        this.mContext = context;
    }

    private Context mContext;

    @Override
    public void run() {
        Log.d(UploadService.TAG, "checking for stuck uploads");

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Recordings.UPLOAD_STATUS, Recording.Status.NOT_YET_UPLOADED);
        final int changed = mContext.getContentResolver().update(
                Content.RECORDINGS.uri,
                cv,
                "upload_status = ?",
                new String[] {String.valueOf(Recording.Status.UPLOADING)});

        if (changed > 0) {
            Log.d(UploadService.TAG, "fixed "+changed+" crashed uploads");
        }
    }
}
