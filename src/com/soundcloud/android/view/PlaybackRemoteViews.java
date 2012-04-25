package com.soundcloud.android.view;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.view.View;
import android.widget.RemoteViews;

public class PlaybackRemoteViews extends RemoteViews{

    private int mPlayBtnId;
    private int mPauseBtnId;

    private Track mTrack;
    private boolean mIsPlaying;

    public PlaybackRemoteViews(String packageName, int layoutId) {
        this(packageName,layoutId, R.drawable.ic_widget_play_states, R.drawable.ic_widget_pause_states);
    }
    public PlaybackRemoteViews(String packageName, int layoutId, int playBtnId, int pauseBtnId) {
        super(packageName, layoutId);
        mPlayBtnId = playBtnId;
        mPauseBtnId = pauseBtnId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public PlaybackRemoteViews(Parcel parcel) {
        super(parcel);
    }

    public void linkButtonsNotification(Context context) {
        linkButtons(context);

        final ComponentName name = new ComponentName(context, CloudPlaybackService.class);
        final Intent close = new Intent(CloudPlaybackService.CLOSE_ACTION).setComponent(name);
        setOnClickPendingIntent(R.id.close, PendingIntent.getService(context,
                0 /* requestCode */, close, 0 /* flags */));
    }

    public void linkButtonsWidget(Context context, long trackId, long userId, boolean userFavorite) {
        linkButtons(context);

        final ComponentName name = new ComponentName(context, CloudPlaybackService.class);
        final Intent player = new Intent(Actions.PLAYER).addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        // go to player
        setOnClickPendingIntent(R.id.title_txt, PendingIntent.getActivity(context, 0, player, 0));
        if (trackId != -1) {
            final Intent browser = new Intent(context, UserBrowser.class).putExtra("userId", userId);
            // go to user
            setOnClickPendingIntent(R.id.user_txt,
                    PendingIntent.getActivity(context, 0, browser, PendingIntent.FLAG_UPDATE_CURRENT));

            final Intent toggleLike = new Intent(
                    userFavorite ?
                            CloudPlaybackService.REMOVE_FAVORITE_ACTION :
                            CloudPlaybackService.ADD_FAVORITE_ACTION)
                    .setComponent(name)
                    .putExtra(CloudPlaybackService.EXTRA_TRACK_ID, trackId);

            // toggle like
            setOnClickPendingIntent(R.id.btn_favorite, PendingIntent.getService(context,
                    0 /* requestCode */, toggleLike, PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }

    private void linkButtons(Context context) {
        // Connect up various buttons and touch events
        final ComponentName name = new ComponentName(context, CloudPlaybackService.class);

        final Intent previous = new Intent(CloudPlaybackService.PREVIOUS_ACTION).setComponent(name);
        setOnClickPendingIntent(R.id.prev, PendingIntent.getService(context,
                0 /* requestCode */, previous, 0 /* flags */));

        final Intent pause = new Intent(CloudPlaybackService.TOGGLEPAUSE_ACTION).setComponent(name);
        setOnClickPendingIntent(R.id.pause, PendingIntent.getService(context,
                0 /* requestCode */, pause, 0 /* flags */));

        final Intent next = new Intent(CloudPlaybackService.NEXT_ACTION).setComponent(name);
        setOnClickPendingIntent(R.id.next, PendingIntent.getService(context,
                0 /* requestCode */, next, 0 /* flags */));
    }

    @Deprecated
    public void setNotification(Track track, boolean isPlaying) {
        mTrack = track;
        mIsPlaying = isPlaying;
        setCurrentTrackTitle(track.title);
        setCurrentUsername(track.user.username);
    }

    public boolean isAlreadyNotifying(Track track, boolean isPlaying){
        return track == mTrack && isPlaying == mIsPlaying;
    }

    public void setCurrentTrackTitle(CharSequence title) {
        setTextViewText(R.id.title_txt, title);
    }

    public void setCurrentUsername(CharSequence username) {
        setTextViewText(R.id.user_txt, username);
        setViewVisibility(R.id.by_txt, View.VISIBLE);
        setViewVisibility(R.id.user_txt, View.VISIBLE);
    }

    public void setPlaybackStatus(boolean playing) {
        setImageViewResource(R.id.pause, playing ? mPauseBtnId : mPlayBtnId);
    }

    public void setIcon(Bitmap icon){
        setImageViewBitmap(R.id.icon,icon);
        setViewVisibility(R.id.icon,View.VISIBLE);
    }

    public void clearIcon(){
        setViewVisibility(R.id.icon,View.GONE);
    }
}
