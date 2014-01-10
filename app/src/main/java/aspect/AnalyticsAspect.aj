import com.soundcloud.android.analytics.AnalyticsEngine;
import com.soundcloud.android.events.*;
import android.app.Activity;

aspect AnalyticsAspect {

    private static final String TAG = AnalyticsAspect.class.getSimpleName();

    pointcut activityOnCreate(Activity activity): execution(* Activity.onCreate(..)) && target(activity);
    pointcut activityOnResume(Activity activity): execution(* Activity.onResume(..)) && target(activity);
    pointcut activityOnPause(Activity activity): execution(* Activity.onPause(..)) && target(activity);

    before(Activity activity): activityOnCreate(activity) {
        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnCreate(activity.getClass()));
    }

    before(Activity activity): activityOnResume(activity) {
        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnResume(activity.getClass()));
    }

    before(Activity activity): activityOnPause(activity) {
        Event.ACTIVITY_EVENT.publish(ActivityLifeCycleEvent.forOnPause(activity.getClass()));
    }
}
