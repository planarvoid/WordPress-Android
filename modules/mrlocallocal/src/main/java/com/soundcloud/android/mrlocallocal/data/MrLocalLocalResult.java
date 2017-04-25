package com.soundcloud.android.mrlocallocal.data;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MrLocalLocalResult {
    public abstract boolean wasSuccessful();
    public abstract boolean canBeRetried();
    public abstract String message();

    public static MrLocalLocalResult success() {
        return new AutoValue_MrLocalLocalResult(true, false, "🚀 MrLocalLocal verification successful!");
    }

    public static MrLocalLocalResult error(boolean canBeRetried, String message) {
        return new AutoValue_MrLocalLocalResult(false, canBeRetried, message);
    }

}
