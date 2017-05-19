package com.soundcloud.android.deeplinks;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;
import android.support.annotation.Nullable;

@AutoValue
abstract class ResolveResult {

    abstract boolean success();
    abstract Optional<Urn> urn();
    abstract Optional<Uri> uri();
    abstract Optional<Exception> exception();

    public static ResolveResult succes(Urn urn) {
        return new AutoValue_ResolveResult(true, Optional.of(urn), Optional.absent(), Optional.absent());
    }

    public static ResolveResult error(Uri uri, @Nullable Exception exception) {
        return new AutoValue_ResolveResult(false, Optional.absent(), Optional.of(uri), Optional.fromNullable(exception));
    }
}
