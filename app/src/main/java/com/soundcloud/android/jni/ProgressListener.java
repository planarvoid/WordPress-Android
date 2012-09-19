package com.soundcloud.android.jni;

import com.soundcloud.android.service.upload.UserCanceledException;

public interface ProgressListener {
    void onProgress(long current, long max) throws UserCanceledException;
}
