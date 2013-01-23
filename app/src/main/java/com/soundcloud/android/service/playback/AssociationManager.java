package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.AddAssociationTask;
import com.soundcloud.android.task.AssociatedSoundTask;
import com.soundcloud.android.task.RemoveAssociationTask;
import com.soundcloud.api.Endpoints;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class AssociationManager {
    private Context mContext;
    private ScModelManager mModelManager;

    public AssociationManager(Context context) {
        mContext = context;
        mModelManager = SoundCloudApplication.MODEL_MANAGER;
    }

    void setLike(@Nullable Playable playable, boolean like) {
        if (playable == null) return;
        onLikeStatusSet(playable, like);
        AssociatedSoundTask task = like ? new AddAssociationTask(getApp(), playable) : new RemoveAssociationTask(getApp(), playable);
        task.setOnAssociatedListener(likeListener);
        task.execute(Endpoints.MY_FAVORITE);
    }

    void setRepost(@Nullable Playable playable, boolean repost) {
        if (playable == null) return;
        onRepostStatusSet(playable, repost);
        AssociatedSoundTask task = repost ? new AddAssociationTask(getApp(), playable) : new RemoveAssociationTask(getApp(), playable);
        task.setOnAssociatedListener(repostListener);
        // resolve the playable content URI to its API endpoint
        String contentPath = playable.toUri().getPath();
        Content repostContent = Content.match(Content.ME_REPOSTS.uriPath + contentPath);
        task.execute(repostContent.remoteUri);
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
        mModelManager.cache(playable, ScResource.CacheUpdateMode.NONE);

        Intent intent = new Intent(Playable.ACTION_TRACK_ASSOCIATION_CHANGED)
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
            playable = (Playable) SoundCloudApplication.MODEL_MANAGER.cache(playable, ScResource.CacheUpdateMode.NONE);
            if (changed && playable.likes_count > ScModel.NOT_SET) {
                if (isAssociated) {
                    playable.likes_count += 1;
                } else {
                    playable.likes_count -= 1;
                }
            }
            onLikeStatusSet(playable, isAssociated);
            updateLocalState(playable, Content.ME_LIKES.uri, isAssociated);
        }
    };

    private final AssociatedSoundTask.AssociatedListener repostListener = new AssociatedSoundTask.AssociatedListener() {
        @Override
        public void onNewStatus(Playable playable, boolean isAssociated, boolean changed) {
            playable = (Playable) SoundCloudApplication.MODEL_MANAGER.cache(playable, ScResource.CacheUpdateMode.NONE);
            if (changed && playable.reposts_count > ScModel.NOT_SET) {
                if (isAssociated) {
                    playable.reposts_count += 1;
                } else {
                    playable.reposts_count -= 1;
                }
            }
            onRepostStatusSet(playable, isAssociated);
            updateLocalState(playable, Content.ME_TRACK_REPOSTS.uri, isAssociated);
        }
    };

    private void updateLocalState(Playable playable, Uri uri, boolean isAssociated) {
        if (isAssociated) {
            mContext.getContentResolver().insert(uri, playable.buildContentValues());
        } else {
            // TODO: this won't work for playlists
            mContext.getContentResolver().delete(uri, "item_id = ?", new String[]{
                String.valueOf(playable.id),
            });
        }
    }
}
