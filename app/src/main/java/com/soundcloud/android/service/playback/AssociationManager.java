package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.event.Event;
import com.soundcloud.android.task.AddAssociationTask;
import com.soundcloud.android.task.AssociatedSoundTask;
import com.soundcloud.android.task.RemoveAssociationTask;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AssociationManager {
    private Context mContext;
    private ScModelManager mModelManager;

    public AssociationManager(Context context) {
        this(context,SoundCloudApplication.MODEL_MANAGER);
    }

    public AssociationManager(Context context, ScModelManager modelManager) {
        mContext = context;
        mModelManager = modelManager;
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
            if (changed && playable.likes_count > ScModel.NOT_SET) {
                if (isAssociated) {
                    playable.likes_count += 1;
                } else {
                    playable.likes_count -= 1;
                }
            }
            onLikeStatusSet(playable, isAssociated);
            updateLocalState(playable, Content.ME_LIKES.uri, isAssociated);

            Event.LIKE_CHANGED.fire(playable);
        }
    };

    private final AssociatedSoundTask.AssociatedListener repostListener = new AssociatedSoundTask.AssociatedListener() {
        @Override
        public void onNewStatus(Playable playable, boolean isAssociated, boolean changed) {
            playable = (Playable) mModelManager.cache(playable, ScResource.CacheUpdateMode.NONE);
            if (changed && playable.reposts_count > ScModel.NOT_SET) {
                if (isAssociated) {
                    playable.reposts_count += 1;
                } else {
                    playable.reposts_count -= 1;
                }
            }
            onRepostStatusSet(playable, isAssociated);
            updateLocalState(playable, Content.ME_REPOSTS.uri, isAssociated);

            Event.REPOST_CHANGED.fire(playable);
        }
    };

    private void updateLocalState(Playable playable, Uri uri, boolean isAssociated) {
        if (isAssociated) {
            mContext.getContentResolver().insert(uri, playable.buildContentValues());
        } else {
            mContext.getContentResolver().delete(uri, "item_id = ? AND " +
                    DBHelper.CollectionItems.RESOURCE_TYPE + " = ?", new String[]{
                    String.valueOf(playable.id), String.valueOf(playable.getTypeId())
            });

            // quick and dirty way to remove reposts from
            if (uri.equals(Content.ME_REPOSTS.uri)){

                Activity.Type activityType = (playable instanceof Track) ? Activity.Type.TRACK_REPOST :
                        Activity.Type.PLAYLIST_REPOST;

                mContext.getContentResolver().delete(Content.ME_SOUND_STREAM.uri,
                        DBHelper.Activities.USER_ID + " = ? AND " + DBHelper.Activities.SOUND_ID + " = ? AND " +
                                DBHelper.ActivityView.TYPE + " = ?",
                        new String[]{String.valueOf(SoundCloudApplication.getUserId()),
                                String.valueOf(playable.id), String.valueOf(activityType)});
            }
        }
    }
}
