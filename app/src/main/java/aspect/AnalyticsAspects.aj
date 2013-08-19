import com.soundcloud.android.utils.Log;
import android.app.Activity;
import com.localytics.android.LocalyticsSession;

aspect AnalyticsAspects {
    private static final String TAG = "AnalyticsAspects";
	private LocalyticsSession localyticsSession;

	pointcut activityOnCreate(Activity activity) : execution(* Activity+.onCreate(..)) && target(activity);
	pointcut activityOnResume(Activity activity) : execution(* Activity+.onResume(..)) && target(activity);
	pointcut activityOnPause(Activity activity) : execution(* Activity+.onPause(..)) && target(activity);

    after(Activity activity) : activityOnCreate(activity) || activityOnResume(activity)  {

        Log.i(TAG, "Opening localytics session");
        if(localyticsSession == null){
            Log.i(TAG, "Localytics session is null initialising");
            localyticsSession = new LocalyticsSession(activity.getApplicationContext());
        } else {
            Log.i(TAG, "Localytics session is not null, reusing");
        }
        localyticsSession.open();
        localyticsSession.upload();
    }

    after(Activity activity) : activityOnPause(activity) {

        Log.i(TAG, "Closing localytics session");
        if(localyticsSession == null){
            Log.i(TAG, "Localytics session is null initialising");
            localyticsSession = new LocalyticsSession(activity.getApplicationContext());
        } else {
            Log.i(TAG, "Localytics session is not null, reusing");
        }
        localyticsSession.close();
        localyticsSession.upload();
    }
}
