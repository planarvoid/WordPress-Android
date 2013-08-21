import com.soundcloud.android.utils.Log;
import android.app.Activity;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.properties.AnalyticsProperties;

aspect AnalyticAspects {
    private static final String TAG = "AnalyticsAspect";
	private LocalyticsSession localyticsSession;

	pointcut activityOnCreate(Activity activity) : execution(* Activity.onCreate(..)) && target(activity);
	pointcut activityOnResume(Activity activity) : execution(* Activity.onResume(..)) && target(activity);
	pointcut activityOnPause(Activity activity) : execution(* Activity.onPause(..)) && target(activity);

    after(Activity activity) : (activityOnCreate(activity) || activityOnResume(activity)) {
        Log.i(TAG, "Executing after aspect for " + thisJoinPoint.toLongString());
        AnalyticsProperties analyticsProperties = new AnalyticsProperties(activity.getResources());

        if(analyticsProperties.isAnalyticsDisabled()){
            Log.i(TAG, "Analytics disabled, not pushing data");
            return;
        }

        Log.i(TAG, "Opening localytics session");
        if(localyticsSession == null){
            Log.i(TAG, "Localytics session is null initialising");
            localyticsSession = new LocalyticsSession(activity.getApplicationContext(), analyticsProperties.getLocalyticsKey());
        }
        localyticsSession.open();
        localyticsSession.upload();
    }

    before(Activity activity) : activityOnPause(activity) && !within(AnalyticAspects +) {
        Log.i(TAG, "Executing before aspect for " + thisJoinPoint.toLongString());
        AnalyticsProperties analyticsProperties = new AnalyticsProperties(activity.getResources());

        if(analyticsProperties.isAnalyticsDisabled()){
            return;
        }

        Log.i(TAG, "Closing localytics session");
        if(localyticsSession == null){
            Log.i(TAG, "Localytics session is null initialising");
            localyticsSession = new LocalyticsSession(activity.getApplicationContext(), analyticsProperties.getLocalyticsKey());
        }
        localyticsSession.close();
        localyticsSession.upload();
    }
}
