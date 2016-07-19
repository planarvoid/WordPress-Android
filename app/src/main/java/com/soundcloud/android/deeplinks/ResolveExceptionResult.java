package com.soundcloud.android.deeplinks;

import com.google.auto.value.AutoValue;

import android.net.Uri;
import android.support.annotation.Nullable;

@AutoValue
abstract class ResolveExceptionResult {

    public static ResolveExceptionResult from(Uri uri, Exception exception) {
        return new AutoValue_ResolveExceptionResult(uri, exception);
    }

    abstract Uri getUri();

    @Nullable
    abstract Exception getException();
}
