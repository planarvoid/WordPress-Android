package com.soundcloud.android.creators.upload;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

/**
 * Checks for uploads which are stuck in state {@link com.soundcloud.android.model.Recording.Status.UPLOADING}, for
 * example after a service crash.
 */
public class StuckUploadCheck implements Runnable {
    private final Context context;

    public StuckUploadCheck(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        Log.d(UploadService.TAG, "checking for stuck uploads");

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Recordings.UPLOAD_STATUS, Recording.Status.NOT_YET_UPLOADED);
        final int changed = context.getContentResolver().update(
                Content.RECORDINGS.uri,
                cv,
                "upload_status = ?",
                new String[] {String.valueOf(Recording.Status.UPLOADING)});

        if (changed > 0) {
            Log.d(UploadService.TAG, "fixed "+changed+" crashed uploads");
        }
    }
}
