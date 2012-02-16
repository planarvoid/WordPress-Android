package com.soundcloud.android.view;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

public class PlaybackRemoteViews extends RemoteViews{

    public PlaybackRemoteViews(String packageName, int layoutId) {
        super(packageName, layoutId);
    }

    public PlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    public void linkButtons(Context context, long trackId, long userId, boolean userFavorite) {
        linkButtons(context,trackId,userId,userFavorite,null);
    }

    public void linkButtons(Context context, long trackId, long userId, boolean userFavorite, String extraFlag) {
        // Connect up various buttons and touch events
        final ComponentName name = new ComponentName(context, CloudPlaybackService.class);
        final Intent previous = new Intent(CloudPlaybackService.PREVIOUS_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(extraFlag)) previous.putExtra(extraFlag,true);
        setOnClickPendingIntent(R.id.prev, PendingIntent.getService(context,
                0 /* requestCode */, previous, 0 /* flags */));

        final Intent toggle = new Intent(CloudPlaybackService.TOGGLEPAUSE_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(extraFlag)) toggle.putExtra(extraFlag,true);
        setOnClickPendingIntent(R.id.pause, PendingIntent.getService(context,
                0 /* requestCode */, toggle, 0 /* flags */));

        final Intent next = new Intent(CloudPlaybackService.NEXT_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(extraFlag)) next.putExtra(extraFlag,true);
        setOnClickPendingIntent(R.id.next, PendingIntent.getService(context,
                0 /* requestCode */, next, 0 /* flags */));

        final Intent player = new Intent(Actions.PLAYER).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        if (!TextUtils.isEmpty(extraFlag)) player.putExtra(extraFlag,true);
        setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context, 0, player, 0));

        final Intent close = new Intent(CloudPlaybackService.STOP_ACTION).setComponent(name);
        if (!TextUtils.isEmpty(extraFlag)) close.putExtra(extraFlag,true);
        setOnClickPendingIntent(R.id.close, PendingIntent.getService(context,
                0 /* requestCode */, close, 0 /* flags */));

        if (trackId != -1) {
            final Intent browser = new Intent(context, UserBrowser.class).putExtra("userId", userId);
            if (!TextUtils.isEmpty(extraFlag)) browser.putExtra(extraFlag,true);
            setOnClickPendingIntent(R.id.user_txt,
                    PendingIntent.getActivity(context, 0, browser, PendingIntent.FLAG_UPDATE_CURRENT));

            final Intent toggleLike = new Intent(
                    userFavorite ?
                            CloudPlaybackService.REMOVE_FAVORITE :
                            CloudPlaybackService.ADD_FAVORITE)
                    .setComponent(name)
                    .putExtra("trackId", trackId);
            if (!TextUtils.isEmpty(extraFlag)) toggleLike.putExtra(extraFlag,true);
            setOnClickPendingIntent(R.id.btn_favorite, PendingIntent.getService(context,
                    0 /* requestCode */, toggleLike, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    public void setCurrentTrack(CharSequence title, CharSequence username) {
        setTextViewText(R.id.title_txt, title);
        setTextViewText(R.id.user_txt, username);
        setViewVisibility(R.id.by_txt, View.VISIBLE);
        setViewVisibility(R.id.user_txt, View.VISIBLE);
    }

    public void setIcon(Bitmap icon){
        setImageViewBitmap(R.id.icon, icon);
        setViewVisibility(R.id.icon,View.VISIBLE);
    }

    public void hideIcon(){
        setViewVisibility(R.id.icon,View.GONE);
    }

    public void setPlaybackStatus(boolean playing) {
        setImageViewResource(R.id.pause, playing ? R.drawable.ic_widget_pause_states : R.drawable.ic_widget_play_states);
    }
}
