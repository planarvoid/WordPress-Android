package com.soundcloud.android.analytics;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.activity.settings.Settings;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collection;

/**
 * The engine which drives sending analytics. Important that all analytics providers to this engine
 * do not rely on singletons to do their work. It should be possible to create multiple providers and open sessions,
 * close sessions and handle the events being sent in a multi-threaded environment.
 *
 * The analytics engine should be used in aspects located in the aspect folder under src/main/java
 */
public class AnalyticsEngine implements SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = AnalyticsEngine.class.getSimpleName();

    private final Collection<AnalyticsProvider> mAnalyticsProviders;
    private final AnalyticsProperties mAnalyticsProperties;
    private boolean mAnalyticsPreferenceEnabled;

    public AnalyticsEngine(Context context){
        this(new AnalyticsProperties(context.getResources()), PreferenceManager.getDefaultSharedPreferences(context),
                new LocalyticsAnalyticsProvider(context.getApplicationContext()));
    }

    @VisibleForTesting
    protected AnalyticsEngine(AnalyticsProperties analyticsProperties, SharedPreferences sharedPreferences, AnalyticsProvider... analyticsProviders) {
        checkArgument(analyticsProviders.length > 0, "Need to provide at least one analytics provider");
        mAnalyticsProviders = Lists.newArrayList(analyticsProviders);
        mAnalyticsProperties = analyticsProperties;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mAnalyticsPreferenceEnabled = sharedPreferences.getBoolean(Settings.ANALYTICS, true);
    }

    public void openSession(){
        if(analyticsIsDisabled()){
            Log.d(TAG, "Analytics disabled, not opening session");
            return;
        }
        Log.i(TAG, "Opening Analytics Session");
        for(AnalyticsProvider analyticsProvider : mAnalyticsProviders){
            analyticsProvider.openSession();
        }
    }

    public void closeSession(){
        if(analyticsIsDisabled()){
            Log.d(TAG, "Analytics disabled, not closing session");
            return;
        }
        Log.i(TAG, "Closing Analytics Session");
        for(AnalyticsProvider analyticsProvider : mAnalyticsProviders){
            analyticsProvider.closeSession();
        }
    }

    private boolean analyticsIsDisabled() {
        return mAnalyticsProperties.isAnalyticsDisabled() || !mAnalyticsPreferenceEnabled;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(Settings.ANALYTICS.equalsIgnoreCase(key)){
            mAnalyticsPreferenceEnabled = sharedPreferences.getBoolean(Settings.ANALYTICS, true);
        }
    }

    @VisibleForTesting
    protected boolean isAnalyticsPreferenceEnabled(){
        return mAnalyticsPreferenceEnabled;
    }
}
