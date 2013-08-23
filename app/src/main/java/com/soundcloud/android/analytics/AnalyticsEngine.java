package com.soundcloud.android.analytics;

import android.content.Context;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.utils.Log;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The engine which drives sending analytics. Important that all analytics providers to this engine
 * do not rely on singletons to do their work. It should be possible to create multiple providers and open sessions,
 * close sessions and handle the events being sent in a multi-threaded environment.
 *
 * The analytics engine should be used in aspects located in the aspect folder under src/main/java
 */
public class AnalyticsEngine {

    private static final String TAG = AnalyticsEngine.class.getSimpleName();

    private final Collection<AnalyticsProvider> mAnalyticsProviders;
    private final AnalyticsProperties mAnalyticsProperties;

    public AnalyticsEngine(Context context){
        this(new AnalyticsProperties(context.getResources()), new LocalyticsAnalyticsProvider(context.getApplicationContext()));
    }

    @VisibleForTesting
    protected AnalyticsEngine(AnalyticsProperties analyticsProperties, AnalyticsProvider... analyticsProviders) {
        checkArgument(analyticsProviders.length > 0, "Need to provide at least one analytics provider");
        mAnalyticsProviders = Lists.newArrayList(analyticsProviders);
        mAnalyticsProperties = analyticsProperties;
    }

    public void openSession(){
        if(mAnalyticsProperties.isAnalyticsDisabled()){
            Log.d(TAG, "Analytics disabled, not opening session");
            return;
        }
        for(AnalyticsProvider analyticsProvider : mAnalyticsProviders){
            analyticsProvider.openSession();
        }
    }

    public void closeSession(){
        if(mAnalyticsProperties.isAnalyticsDisabled()){
            Log.d(TAG, "Analytics disabled, not closing session");
            return;
        }

        for(AnalyticsProvider analyticsProvider : mAnalyticsProviders){
            analyticsProvider.closeSession();
        }
    }

}
