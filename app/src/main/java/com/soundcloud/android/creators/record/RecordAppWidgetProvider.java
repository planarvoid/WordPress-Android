package com.soundcloud.android.creators.record;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class RecordAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "RecordWidget";
    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.soundcloud.android",
                    "com.soundcloud.android.service.record.RecordAppWidgetProvider");

    private static RecordAppWidgetProvider instance;

    public static synchronized RecordAppWidgetProvider getInstance() {
        if (instance == null) {
            instance = new RecordAppWidgetProvider();
        }
        return instance;
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        linkButtons(context, appWidgetIds, false);
    }


    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(THIS_APPWIDGET, views);
        }
    }

    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        return (appWidgetIds.length > 0);
    }


    public void notifyChange(Context context, Intent intent) {
        String action = intent.getAction();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "notify change " + intent);
        }
        if (hasInstances(context)) {
            if (action.equals(SoundRecorder.RECORD_STARTED)) {
                linkButtons(context, null, true);
            } else if (action.equals(SoundRecorder.RECORD_FINISHED)) {
                linkButtons(context, null, false);
            }
        }
    }


    private void linkButtons(Context context, int[] appWidgetIds, boolean isRecording) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_record);
        // Connect up various buttons and touch events
        if (isRecording) {
            views.setImageViewResource(R.id.btn_action, R.drawable.btn_rec_pause_states);
            views.setOnClickPendingIntent(R.id.btn_action,
                    PendingIntent.getActivity(context, 0, new Intent(Actions.RECORD_STOP), PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            views.setImageViewResource(R.id.btn_action, R.drawable.btn_rec_states);
            views.setOnClickPendingIntent(R.id.btn_action,
                    PendingIntent.getActivity(context, 0, new Intent(Actions.RECORD_START), PendingIntent.FLAG_CANCEL_CURRENT));
        }

        pushUpdate(context, appWidgetIds, views);
    }
}
