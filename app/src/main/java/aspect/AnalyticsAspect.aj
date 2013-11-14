import android.app.Activity;
import com.soundcloud.android.analytics.AnalyticsEngine;
import android.content.Context;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackState;
import android.util.Log;

aspect AnalyticAspects {
	private AnalyticsEngine analyticsEngine;

	pointcut activityOnCreate(Activity activity) : execution(* Activity.onCreate(..)) && target(activity);
	pointcut activityOnResume(Activity activity) : execution(* Activity.onResume(..)) && target(activity);
	pointcut activityOnPause(Activity activity) : execution(* Activity.onPause(..)) && target(activity);

    after(Activity activity) : (activityOnCreate(activity) || activityOnResume(activity)) {
        initialiseAnalyticsEngine(activity);
        analyticsEngine.openSessionForActivity();
    }

    before(Activity activity) : activityOnPause(activity) {
        initialiseAnalyticsEngine(activity);
        analyticsEngine.closeSessionForActivity();
    }

    private void initialiseAnalyticsEngine(Context context){
        if(analyticsEngine == null){
            Log.d("Aspect", "Creating aspect analytics engine");
            analyticsEngine = new AnalyticsEngine(context.getApplicationContext());
        }
    }
}
