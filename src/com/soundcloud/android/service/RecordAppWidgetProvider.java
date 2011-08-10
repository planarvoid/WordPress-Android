package com.soundcloud.android.service;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.Main;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class RecordAppWidgetProvider extends AppWidgetProvider {
    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.soundcloud.android",
                    "com.soundcloud.android.RecordAppWidgetProvider");

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
    }

    /**
     * Initialize given widgets to default state
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_record);
        linkButtons(context, views);
        pushUpdate(context, appWidgetIds, views);
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

    private void linkButtons(Context context, RemoteViews views) {
        // Connect up various buttons and touch events
        Intent i = (new Intent(context, Main.class))
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("tabTag", "record");

        views.setOnClickPendingIntent(R.id.btn_action,
                PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT));
    }
}
