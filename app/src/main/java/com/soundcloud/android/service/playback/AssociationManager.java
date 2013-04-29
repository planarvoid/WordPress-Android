package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.SoundAssociationStorage;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.task.AddAssociationTask;
import com.soundcloud.android.task.AssociatedSoundTask;
import com.soundcloud.android.task.RemoveAssociationTask;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;

public class AssociationManager {
    private Context mContext;
    private ScModelManager mModelManager;
    private SoundAssociationStorage mSoundAssocStorage;

    public AssociationManager(Context context) {
        this(context, SoundCloudApplication.MODEL_MANAGER);
    }

    public AssociationManager(Context context, ScModelManager modelManager) {
        mContext = context;
        mModelManager = modelManager;
        mSoundAssocStorage = new SoundAssociationStorage(context);
    }

    void setLike(@Nullable Playable playable, boolean likeAdded) {
        if (playable == null) return;
        onLikeStatusSet(playable, likeAdded);
        pushToRemote(playable, Content.ME_LIKES, likeAdded, likeListener);
    }

    void setRepost(@Nullable Playable playable, boolean repostAdded) {
        if (playable == null) return;
        onRepostStatusSet(playable, repostAdded);
        pushToRemote(playable, Content.ME_REPOSTS, repostAdded, repostListener);
    }

    /**
     * PUT the added/removed like/repost to the API.
     *
     * @param playable the track or playlist that was liked or reposted
     * @param content  the parent content URI
     * @param added    true if the association was added, false if it was removed
     * @param listener the callback for the API call
     */
    private void pushToRemote(Playable playable, Content content, boolean added, AssociatedSoundTask.AssociatedListener listener) {
        AssociatedSoundTask task = added ? new AddAssociationTask(getApp(), playable) : new RemoveAssociationTask(getApp(), playable);
        task.setOnAssociatedListener(listener);
        // resolve the playable content URI to its API endpoint
        String contentPath = playable.toUri().getPath();
        task.execute(Content.match(content.uriPath + contentPath).remoteUri);
    }

    private void onLikeStatusSet(Playable playable, boolean isLike) {
        playable.user_like = isLike;
        onAssociationChanged(playable);
    }

    private void onRepostStatusSet(Playable playable, boolean isRepost) {
        playable.user_repost = isRepost;
        onAssociationChanged(playable);
    }

    private void onAssociationChanged(Playable playable) {
        Intent intent = new Intent(Playable.ACTION_PLAYABLE_ASSOCIATION_CHANGED)
                .putExtra(CloudPlaybackService.BroadcastExtras.id, playable.id)
                .putExtra(CloudPlaybackService.BroadcastExtras.isRepost, playable.user_repost)
                .putExtra(CloudPlaybackService.BroadcastExtras.isLike, playable.user_like)
                .putExtra(CloudPlaybackService.BroadcastExtras.isSupposedToBePlaying, CloudPlaybackService.getState().isSupposedToBePlaying());

        mContext.sendBroadcast(intent);
        PlayerAppWidgetProvider.getInstance().notifyChange(mContext, intent);
    }

    private SoundCloudApplication getApp() {
        return SoundCloudApplication.fromContext(mContext);
    }

    private final AssociatedSoundTask.AssociatedListener likeListener = new AssociatedSoundTask.AssociatedListener() {
        @Override
        public void onNewStatus(Playable playable, boolean isAssociated, boolean changed) {
            playable = (Playable) mModelManager.cache(playable, ScResource.CacheUpdateMode.NONE);
            if (changed) {
                if (isAssociated) {
                    if (playable.isLikesCountSet()) playable.likes_count += 1;
                    mSoundAssocStorage.addLike(playable);
                } else {
                    if (playable.isLikesCountSet()) playable.likes_count -= 1;
                    mSoundAssocStorage.removeLike(playable);
                }
            }
            onLikeStatusSet(playable, isAssociated);
        }
    };

    private final AssociatedSoundTask.AssociatedListener repostListener = new AssociatedSoundTask.AssociatedListener() {
        @Override
        public void onNewStatus(Playable playable, boolean isAssociated, boolean changed) {
            playable = (Playable) mModelManager.cache(playable, ScResource.CacheUpdateMode.NONE);
            if (changed) {
                if (isAssociated) {
                    if (playable.isRepostCountSet()) playable.reposts_count += 1;
                    mSoundAssocStorage.addRepost(playable);
                } else {
                    if (playable.isRepostCountSet()) playable.reposts_count -= 1;
                    mSoundAssocStorage.removeRepost(playable);

                    // quick and dirty way to remove reposts from
                    Activity.Type activityType = (playable instanceof Track) ? Activity.Type.TRACK_REPOST :
                            Activity.Type.PLAYLIST_REPOST;

                    mContext.getContentResolver().delete(Content.ME_SOUND_STREAM.uri,
                            DBHelper.Activities.USER_ID + " = ? AND " + DBHelper.Activities.SOUND_ID + " = ? AND " +
                                    DBHelper.ActivityView.TYPE + " = ?",
                            new String[]{String.valueOf(SoundCloudApplication.getUserId()),
                                    String.valueOf(playable.id), String.valueOf(activityType)});
                }
            }
            onRepostStatusSet(playable, isAssociated);
        }
    };
}
