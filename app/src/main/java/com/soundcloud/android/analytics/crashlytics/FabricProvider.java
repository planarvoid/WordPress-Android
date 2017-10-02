package com.soundcloud.android.analytics.crashlytics;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Lists;
import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Kit;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
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

    public static void initialize(Context context, ApplicationProperties applicationProperties) {
        List<Kit> kits = Lists.newArrayList(new Crashlytics(), new Answers());
        if (applicationProperties.shouldReportNativeCrashes()) {
            try {
                kits.add((Kit) Class.forName("com.crashlytics.android.ndk.CrashlyticsNdk").newInstance());
            } catch (Exception e) {
                ErrorUtils.handleSilentException("Failed to load CrashlyticsNdk", e);
            }
        }
        Fabric.with(context, kits.toArray(new Kit[kits.size()]));
    }

    boolean isInitialized() {
        return Fabric.isInitialized();
    }

    Executor getExecutor() {
        return executor;
    }

    CrashlyticsCore getCrashlyticsCore() {
        return Fabric.getKit(Crashlytics.class).core;
    }
}
