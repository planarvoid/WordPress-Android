package com.soundcloud.android.service;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlaybackActivityStarter;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Track;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

    /**
     * Player Widget
     */
    public class PlayerAppWidgetProvider extends AppWidgetProvider {
        static final String TAG = "PlayerWidget";

        public static final String CMDAPPWIDGETUPDATE = "playerwidgetupdate";

        private RemoteViews mCurrentViews;

        static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.soundcloud.android",
                    "com.soundcloud.android.service.PlayerAppWidgetProvider");

        private static PlayerAppWidgetProvider sInstance;

        public static synchronized PlayerAppWidgetProvider getInstance() {
            if (sInstance == null) {
                sInstance = new PlayerAppWidgetProvider();
            }
            return sInstance;
        }

        @Override
        public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
            defaultAppWidget(context, appWidgetIds);

            // Send broadcast intent to any running CloudPlaybackService so it can
            // wrap around with an immediate update.
            Intent updateIntent = new Intent(CloudPlaybackService.SERVICECMD);
            updateIntent.putExtra(CloudPlaybackService.CMDNAME,
                    PlayerAppWidgetProvider.CMDAPPWIDGETUPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            context.sendBroadcast(updateIntent);
        }

        /**
         * Initialize given widgets to default state
         */
        private void defaultAppWidget(Context context, int[] appWidgetIds) {
            context.getResources();
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_player);

            views.setTextViewText(R.id.title_txt, "Touch to open SoundCloud");
            views.setViewVisibility(R.id.by_txt, View.GONE);
            views.setViewVisibility(R.id.user_txt, View.GONE);

            // initialize controls
            linkButtons(context, views, false, null);
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

        /**
         * Check against {@link AppWidgetManager} if there are any instances of this widget.
         */
        private boolean hasInstances(Context context) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
            return (appWidgetIds.length > 0);
        }

        private boolean mCurrentTrackError = false;
        private long mCurrentTrackId = -1;

        /**
         * Handle a change notification coming over from {@link CloudPlaybackService}
         */
        void notifyChange(CloudPlaybackService service, String what) {
            Log.i(TAG,"notify change "+ service.getTrackId() + " " + what);

            if (hasInstances(service)) {
                if (what.equals(CloudPlaybackService.META_CHANGED)||
                        what.equals(CloudPlaybackService.PLAYBACK_COMPLETE) ||
                        what.equals(CloudPlaybackService.PLAYSTATE_CHANGED) ||
                        what.equals(CloudPlaybackService.INITIAL_BUFFERING) ||
                        what.equals(CloudPlaybackService.BUFFERING) ||
                        what.equals(CloudPlaybackService.BUFFERING_COMPLETE) ||
                        what.equals(CloudPlaybackService.TRACK_ERROR) ||
                        what.equals(CloudPlaybackService.FAVORITE_SET)){
                    performUpdate(service, null, what);
                }
            }
        }

        void performUpdate(CloudPlaybackService service, int[] appWidgetIds) {
            performUpdate(service,appWidgetIds,CloudPlaybackService.PLAYSTATE_CHANGED);
        }

        /**
         * Update all active widget instances by pushing changes
         */
        void performUpdate(CloudPlaybackService service, int[] appWidgetIds, String what) {
            final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.appwidget_player);

            final boolean playing = service.isPlaying();
            views.setImageViewResource(R.id.pause, playing ? R.drawable.ic_widget_pause_states : R.drawable.ic_widget_play_states);



            Track mCurrentTrack = service.getTrack();


            if (mCurrentTrack == null)
                return;

            views.setImageViewResource(R.id.btn_favorite, mCurrentTrack.user_favorite ? R.drawable.ic_widget_favorited_states : R.drawable.ic_widget_favorite_states);


            if (mCurrentTrackId != mCurrentTrack.id){

                mCurrentTrackId = mCurrentTrack.id;

                views.setTextViewText(R.id.title_txt, mCurrentTrack.title);
                views.setTextViewText(R.id.user_txt, mCurrentTrack.user.username);

                views.setViewVisibility(R.id.by_txt, View.VISIBLE);
                views.setViewVisibility(R.id.user_txt, View.VISIBLE);

            }

            linkButtons(service, views, playing, mCurrentTrack);
            pushUpdate(service, appWidgetIds, views);
            mCurrentViews = views;
        }

        /**
         * Link up various button actions using {@link PendingIntents}.
         *
         * @param playerActive True if player is active in background, which means
         *            widget click will launch {@link ScPlaybackActivityStarter},
         *            otherwise we launch {@link MusicBrowserActivity}.
         */
        private void linkButtons(Context context, RemoteViews views, boolean playerActive, Track track) {
            // Connect up various buttons and touch events
            Intent intent;
            PendingIntent pendingIntent;

            final ComponentName serviceName = new ComponentName(context, CloudPlaybackService.class);

            if (playerActive) {
                intent = new Intent(context, ScPlaybackActivityStarter.class);
                pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, intent, 0 /* no flags */);
                //views.setOnClickPendingIntent(R.id., pendingIntent);
            } else {
                intent = new Intent(context, Dashboard.class);
                pendingIntent = PendingIntent.getActivity(context,
                        0 /* no requestCode */, intent, 0 /* no flags */);
                //views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
            }

            intent = new Intent(CloudPlaybackService.PREVIOUS_ACTION);
            intent.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.prev, pendingIntent);

            intent = new Intent(CloudPlaybackService.TOGGLEPAUSE_ACTION);
            intent.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.pause, pendingIntent);

            intent = new Intent(CloudPlaybackService.NEXT_ACTION);
            intent.setComponent(serviceName);
            pendingIntent = PendingIntent.getService(context,
                    0 /* no requestCode */, intent, 0 /* no flags */);
            views.setOnClickPendingIntent(R.id.next, pendingIntent);

            intent = new Intent(context, ScPlayer.class);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.setAction(Intent.ACTION_MAIN);

            pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.title_txt, pendingIntent);

            if (track != null){
                intent = new Intent(context, UserBrowser.class);
                intent.putExtra("userId", track.user.id);
                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.user_txt, pendingIntent);

                if (track.user_favorite){
                    intent = new Intent(CloudPlaybackService.REMOVE_FAVORITE);
                } else {
                    intent = new Intent(CloudPlaybackService.ADD_FAVORITE);
                }

                intent.setComponent(serviceName);
                intent.putExtra("trackId", track.id);
                pendingIntent = PendingIntent.getService(context,
                        0 /* no requestCode */, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.btn_favorite, pendingIntent);

            }



        }
    }

