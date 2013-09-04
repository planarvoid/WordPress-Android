import android.app.Activity;
import com.soundcloud.android.analytics.AnalyticsEngine;
import android.content.Context;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.State;

aspect AnalyticAspects {
	private AnalyticsEngine analyticsEngine;

	pointcut activityOnCreate(Activity activity) : execution(* Activity.onCreate(..)) && target(activity);
	pointcut activityOnResume(Activity activity) : execution(* Activity.onResume(..)) && target(activity);
	pointcut activityOnPause(Activity activity) : execution(* Activity.onPause(..)) && target(activity);

    after(Activity activity) : (activityOnCreate(activity) || activityOnResume(activity)) {
        initialiseAnalyticsEngine(activity);
        analyticsEngine.openSession();
    }

    before(Activity activity) : activityOnPause(activity) {
        initialiseAnalyticsEngine(activity);
        analyticsEngine.closeSession();
    }

    private void initialiseAnalyticsEngine(Context context){
        if(analyticsEngine == null){
            analyticsEngine = new AnalyticsEngine(context.getApplicationContext());
        }
    }
}
