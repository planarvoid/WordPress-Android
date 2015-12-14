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

    private Executor executor;

    @Inject
    public FabricProvider() {
    }

    public void initialize(Context context) {
        final Fabric fabric = Fabric.with(context, new Crashlytics(), new Answers());
        this.executor = fabric.getExecutorService();
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
