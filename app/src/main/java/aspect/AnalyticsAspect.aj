import com.soundcloud.android.analytics.AnalyticsEngine;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

aspect AnalyticsAspect {

    private static final String TAG = AnalyticsAspect.class.getSimpleName();

    private AnalyticsEngine analyticsEngine;

    pointcut activityOnCreate(Activity activity): execution(* Activity.onCreate(..)) && target(activity);
    pointcut activityOnResume(Activity activity): execution(* Activity.onResume(..)) && target(activity);
    pointcut activityOnPause(Activity activity): execution(* Activity.onPause(..)) && target(activity);

    after(Activity activity): (activityOnCreate(activity) || activityOnResume(activity)) {
        Log.d(TAG, "Opening session for " + activity.getClass().getSimpleName());
        initialiseAnalyticsEngine(activity);
        analyticsEngine.openSessionForActivity();
    }

    before(Activity activity): activityOnPause(activity) {
        Log.d(TAG, "Closing session for " + activity.getClass().getSimpleName());
        initialiseAnalyticsEngine(activity);
        analyticsEngine.closeSessionForActivity();
    }

    private void initialiseAnalyticsEngine(Context context) {
        if (analyticsEngine == null) {
            Log.d(TAG, "Creating aspect analytics engine for " + context.getClass().getSimpleName());
            analyticsEngine = new AnalyticsEngine(context.getApplicationContext());
        }
    }
}
