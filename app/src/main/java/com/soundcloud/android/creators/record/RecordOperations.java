package com.soundcloud.android.creators.record;

import com.soundcloud.android.Actions;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.upload.UploadService;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class RecordOperations {

    @Inject
    public RecordOperations() {
    }

    public void upload(Context context, Recording recording) {
        final Intent intent = new Intent(context, UploadService.class)
                .setAction(Actions.UPLOAD)
                .putExtra(SoundRecorder.EXTRA_RECORDING, recording);
        context.startService(intent);
    }

    public void cancelUpload(Context context, Recording recording) {
        final Intent intent = new Intent(context, UploadService.class)
                .setAction(Actions.UPLOAD_CANCEL)
                .putExtra(SoundRecorder.EXTRA_RECORDING, recording);
        context.startService(intent);
    }
}
