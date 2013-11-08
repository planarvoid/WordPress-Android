package com.soundcloud.android.associations;


import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.playback.service.CloudPlaybackService;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;

// TODO This will be migrated to an Operations class and use RxJava instead of intents
@Deprecated
public class AssociationManager {
    private Context mContext;
    private ScModelManager mModelManager;
    private SoundAssociationStorage mSoundAssocStorage;
    private PublicCloudAPI mOldCloudAPI;

    public AssociationManager(Context context) {
        this(context, SoundCloudApplication.MODEL_MANAGER);
    }

    public AssociationManager(Context context, ScModelManager modelManager) {
        mContext = context;
        mModelManager = modelManager;
        mSoundAssocStorage = new SoundAssociationStorage();
        mOldCloudAPI = new PublicApi(context);
    }

    public void setLike(@Nullable Playable playable, boolean likeAdded) {
        if (playable == null) return;
        onLikeStatusSet(playable, likeAdded);
        pushToRemote(playable, Content.ME_LIKES, likeAdded, likeListener);
    }

    public void setRepost(@Nullable Playable playable, boolean repostAdded) {
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
        AssociatedSoundTask task = added ? new AddAssociationTask(mOldCloudAPI, playable) : new RemoveAssociationTask(mOldCloudAPI, playable);
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
                .putExtra(CloudPlaybackService.BroadcastExtras.id, playable.getId())
                .putExtra(CloudPlaybackService.BroadcastExtras.isRepost, playable.user_repost)
                .putExtra(CloudPlaybackService.BroadcastExtras.isLike, playable.user_like)
                .putExtra(CloudPlaybackService.BroadcastExtras.isSupposedToBePlaying, CloudPlaybackService.getPlaybackState().isSupposedToBePlaying());

        mContext.sendBroadcast(intent);
        PlayerAppWidgetProvider.getInstance().notifyChange(mContext, intent);
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
                                    String.valueOf(playable.getId()), String.valueOf(activityType)});
                }
            }
            onRepostStatusSet(playable, isAssociated);
        }
    };
}
