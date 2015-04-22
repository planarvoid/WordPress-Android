package com.soundcloud.android.framework.helpers;

import static com.soundcloud.android.framework.helpers.OfflineContentHelper.clearOfflineContent;

import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.crypto.Obfuscator;
import rx.functions.Action1;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigurationHelper {

    private final static String OFFLINE_CONTENT = "offline_sync";
    private final static String OFFLINE_UPSELL = "offline_sync_upsell";

    public static void enableOfflineContent(Context context) {
        enableFeature(context, OFFLINE_CONTENT);
    }

    public static void enableUpsell(Context context) {
        enableFeature(context, OFFLINE_UPSELL);
    }

    private static void enableFeature(Context context, final String feature) {
        final FeatureStorage featureStorage = getFeatureStorage(context);

        featureStorage.update(feature, true);

        featureStorage.getUpdates(feature)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean enabled) {
                        if (!enabled) {
                            featureStorage.update(feature, true);
                        }
                    }
                })
                .subscribe();
    }

    public static void disableOfflineContent(Context context) {
        getFeatureStorage(context).update(OFFLINE_CONTENT, false);
    }

    public static void resetOfflineSyncState(Context context) {
        disableOfflineContent(context);
        clearOfflineContent(context);
    }

    private static FeatureStorage getFeatureStorage(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("features_settings", Context.MODE_PRIVATE);
        return new FeatureStorage(sharedPreferences, new Obfuscator());
    }

}
