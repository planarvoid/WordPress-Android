package com.soundcloud.android.analytics.crashlytics;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import io.fabric.sdk.android.Fabric;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executor;

@Singleton
public class FabricProvider {

    private final Executor executor;

    @Inject
    public FabricProvider(Context context) {
        // Note : Fabric is only initialized the first time you call start,
        // so calling it multiple times won’t cause any issues.
        // https://docs.fabric.io/android/fabric/overview.html
        executor = Fabric.with(context).getExecutorService();
    }

    public static void initialize(Context context) {
        Fabric.with(context, new Crashlytics(), new Answers());
    }

    public boolean isInitialized() {
        return Fabric.isInitialized();
    }

    Executor getExecutor() {
        return executor;
    }

    Answers getAnswers() {
        return Fabric.getKit(Answers.class);
    }

    CrashlyticsCore getCrashlyticsCore() {
        return Fabric.getKit(Crashlytics.class).core;
    }
}
