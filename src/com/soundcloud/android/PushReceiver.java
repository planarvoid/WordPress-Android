package com.soundcloud.android;

import com.soundcloud.android.activity.Main;
import com.soundcloud.android.service.SyncAdapterService;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PushReceiver extends BroadcastReceiver {
        public static final String TAG = PushReceiver.class.getSimpleName();
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received intent: " + intent.toString());
            String action = intent.getAction();

            if (action.equals(PushManager.ACTION_PUSH_RECEIVED)) {

                int id = intent.getIntExtra(PushManager.EXTRA_NOTIFICATION_ID, 0);

                Log.i(TAG, "Received push notification. Alert: "
                        + intent.getStringExtra(PushManager.EXTRA_ALERT)
                        + " [NotificationID="+id+"]");

                logPushExtras(intent);

                SyncAdapterService.createDashboardNotification(context,"Received SoundCloud Notification","Received SoundCloud Notification",intent.getStringExtra(PushManager.EXTRA_ALERT),false);

            } else if (action.equals(PushManager.ACTION_NOTIFICATION_OPENED)) {

                Log.i(TAG, "User clicked notification. Message: " + intent.getStringExtra(PushManager.EXTRA_ALERT));

                logPushExtras(intent);

                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.setClass(UAirship.shared().getApplicationContext(), Main.class);
                launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            UAirship.shared().getApplicationContext().startActivity(launch);

		} else if (action.equals(PushManager.ACTION_REGISTRATION_FINISHED)) {
            Log.i(TAG, "Registration complete. APID:" + intent.getStringExtra(PushManager.EXTRA_APID)
                    + ". Valid: " + intent.getBooleanExtra(PushManager.EXTRA_REGISTRATION_VALID, false));
		}

	}

	/**
	 * Log the values sent in the payload's "extra" dictionary.
	 *
	 * @param intent A PushManager.ACTION_NOTIFICATION_OPENED or ACTION_PUSH_RECEIVED intent.
	 */
	private void logPushExtras(Intent intent) {
        Set<String> keys = intent.getExtras().keySet();
        for (String key : keys) {

            //ignore standard C2DM extra keys
            List<String> ignoredKeys = (List<String>) Arrays.asList(
                    "collapse_key",//c2dm collapse key
                    "from",//c2dm sender
                    PushManager.EXTRA_NOTIFICATION_ID,//int id of generated notification (ACTION_PUSH_RECEIVED only)
                    PushManager.EXTRA_PUSH_ID,//internal UA push id
                    PushManager.EXTRA_ALERT);//ignore alert
            if (ignoredKeys.contains(key)) {
                continue;
            }
            Log.i(TAG, "Push Notification Extra: ["+key+" : " + intent.getStringExtra(key) + "]");
        }
	}
}