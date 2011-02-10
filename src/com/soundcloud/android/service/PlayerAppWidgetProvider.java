package com.soundcloud.android.service;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.ScPlaybackActivityStarter;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.objects.Track;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

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
            final Resources res = context.getResources();
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_player);
            
            
            // initialize controls
            
            linkButtons(context, views, false);
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
                        what.equals(CloudPlaybackService.TRACK_ERROR) ){
                    performUpdate(service, null, what);
                }          
            }
        }
        
        void updatePosition(CloudPlaybackService service, int max, long position){
            Log.i(TAG,"Update position " + position + " of " + max);
            if (mCurrentViews != null){
                mCurrentViews.setProgressBar(R.id.progress_bar, max, max, false);
                pushUpdate(service, null, mCurrentViews);
            }
        }
        
        void performUpdate(CloudPlaybackService service, int[] appWidgetIds) {
            performUpdate(service,appWidgetIds,CloudPlaybackService.PLAYSTATE_CHANGED);
        }
        
        /**
         * Update all active widget instances by pushing changes 
         */
        void performUpdate(CloudPlaybackService service, int[] appWidgetIds, String what) {
            final Resources res = service.getResources();
            final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.appwidget_player);
            
            if (what.equals(CloudPlaybackService.META_CHANGED)) {
                mCurrentTrackError = false;
            } else if (what.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                if (service.isPlaying() == false) {
                    hideUnplayable(service,views);
                    mCurrentTrackError = false;
                }
            } else if (what.equals(CloudPlaybackService.INITIAL_BUFFERING)) {
                mCurrentTrackError = false;
                hideUnplayable(service,views);
            } else if (what.equals(CloudPlaybackService.BUFFERING)) {
                hideUnplayable(service,views);
            } else if (what.equals(CloudPlaybackService.BUFFERING_COMPLETE)) {
                // clearSeekVars();
            } else if (what.equals(CloudPlaybackService.TRACK_ERROR)) {
                mCurrentTrackError = true;
                showUnplayable(service,views);
            }             
            
            
            final boolean playing = service.isPlaying();
            views.setImageViewResource(R.id.pause, playing ? R.drawable.ic_widget_pause_states : R.drawable.ic_widget_play_states);
            
            if (mCurrentTrackId != service.getTrackId()){
                Track mCurrentTrack = service.getTrack();
                mCurrentTrackId = mCurrentTrack.id;
                
                views.setProgressBar(R.id.progress_bar, mCurrentTrack.duration, mCurrentTrack.duration, false);
                views.setTextViewText(R.id.title_txt, mCurrentTrack.title);
                views.setTextViewText(R.id.user_txt, mCurrentTrack.user.username);
                
                
                URL url;
                Bitmap b = null;
                try {
                    url = new URL(mCurrentTrack.waveform_url);
                    URLConnection connection = url.openConnection();
                    b = (Bitmap) ((SoundCloudApplication) service.getApplication()).getBitmapHandler().getContent(connection);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
               
                if (b != null){
                    Matrix m = new Matrix();
                    //m.setScale(1, 2);
                    views.setImageViewBitmap(R.id.progress_overlay, b);
                    
                }
                
            }
            
            // Link actions buttons to intents
            linkButtons(service, views, playing);
            pushUpdate(service, appWidgetIds, views);
            mCurrentViews = views;
        }
        
        
        private void showUnplayable(CloudPlaybackService service,RemoteViews views) {
            views.setTextViewText(R.id.unplayable_txt, service.getResources().getText(R.string.player_not_streamable));
            
            if ( service.getTrack() == null || CloudUtils.isTrackPlayable(service.getTrack())) { // playback
                // error
                views.setTextViewText(R.id.unplayable_txt, service.getResources().getText(R.string.player_error));
            } else {
                views.setTextViewText(R.id.unplayable_txt, service.getResources().getText(R.string.player_not_streamable));
            }
            
            views.setViewVisibility(R.id.playable_layout, View.GONE);
            views.setViewVisibility(R.id.unplayable_layout, View.VISIBLE);
        }

        private void hideUnplayable(CloudPlaybackService service, RemoteViews views) {
            views.setViewVisibility(R.id.playable_layout, View.VISIBLE);
            views.setViewVisibility(R.id.unplayable_layout, View.GONE);
        }

        /**
         * Link up various button actions using {@link PendingIntents}.
         * 
         * @param playerActive True if player is active in background, which means
         *            widget click will launch {@link ScPlaybackActivityStarter},
         *            otherwise we launch {@link MusicBrowserActivity}.
         */
        private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
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
            
        }
    }

