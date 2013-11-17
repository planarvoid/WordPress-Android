package com.soundcloud.android.creators.record.jni;

import com.soundcloud.android.creators.upload.UserCanceledException;

public interface ProgressListener {
    void onProgress(long current, long max) throws UserCanceledException;
}
