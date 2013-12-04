import com.soundcloud.android.analytics.AnalyticsEngine;

import android.app.Activity;
import android.util.Log;

aspect AnalyticsAspect {

    private static final String TAG = AnalyticsAspect.class.getSimpleName();

    pointcut activityOnCreate(Activity activity): execution(* Activity.onCreate(..)) && target(activity);
    pointcut activityOnResume(Activity activity): execution(* Activity.onResume(..)) && target(activity);
    pointcut activityOnPause(Activity activity): execution(* Activity.onPause(..)) && target(activity);

    after(Activity activity): (activityOnCreate(activity) || activityOnResume(activity)) {
        Log.d(TAG, "Opening session for " + activity.getClass().getSimpleName());
        AnalyticsEngine.getInstance(activity).openSessionForActivity();
    }

    before(Activity activity): activityOnPause(activity) {
        Log.d(TAG, "Closing session for " + activity.getClass().getSimpleName());
        AnalyticsEngine.getInstance(activity).closeSessionForActivity();
    }
}
