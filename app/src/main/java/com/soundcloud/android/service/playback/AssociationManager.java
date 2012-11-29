package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Sound;
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

    void setLike(@Nullable Sound sound, boolean like) {
        if (sound == null) return;
        onLikeStatusSet(sound, like);
        AssociatedSoundTask task = like ? new AddAssociationTask(getApp(), sound) : new RemoveAssociationTask(getApp(), sound);
        task.setOnAssociatedListener(likeListener);
        task.execute(Endpoints.MY_FAVORITE);
    }

    void setRepost(@Nullable Sound sound, boolean repost) {
        if (sound == null) return;
        onRepostStatusSet(sound, repost);
        AssociatedSoundTask task = repost ? new AddAssociationTask(getApp(), sound) : new RemoveAssociationTask(getApp(), sound);
        task.setOnAssociatedListener(repostListener);
        task.execute(TempEndpoints.e1.MY_REPOST);
    }

    private void onLikeStatusSet(Sound sound, boolean isLike) {
        sound.user_like = isLike;
        onAssociationChanged(sound);
    }

    private void onRepostStatusSet(Sound sound, boolean isRepost) {
        sound.user_repost = isRepost;
        onAssociationChanged(sound);
    }

    private void onAssociationChanged(Sound sound) {
        mModelManager.cache(sound, ScResource.CacheUpdateMode.NONE);

        Intent intent = new Intent(Sound.ACTION_TRACK_ASSOCIATION_CHANGED)
                .putExtra(CloudPlaybackService.BroadcastExtras.id, sound.id)
                .putExtra(CloudPlaybackService.BroadcastExtras.isRepost, sound.user_repost)
                .putExtra(CloudPlaybackService.BroadcastExtras.isLike, sound.user_like);

        mContext.sendBroadcast(intent);
        PlayerAppWidgetProvider.getInstance().notifyChange(mContext, intent);
    }

    private SoundCloudApplication getApp() {
        return SoundCloudApplication.fromContext(mContext);
    }

    private final AssociatedSoundTask.AssociatedListener likeListener = new AssociatedSoundTask.AssociatedListener() {
        @Override
        public void onNewStatus(Sound sound, boolean isAssociated) {
            onLikeStatusSet(sound, isAssociated);
            updateLocalState(sound, Content.ME_LIKES.uri, isAssociated);
        }
    };

    private final AssociatedSoundTask.AssociatedListener repostListener = new AssociatedSoundTask.AssociatedListener() {
        @Override
        public void onNewStatus(Sound sound, boolean isAssociated) {
            onRepostStatusSet(sound, isAssociated);
            updateLocalState(sound, Content.ME_REPOSTS.uri, isAssociated);
        }
    };

    private void updateLocalState(Sound sound, Uri uri, boolean isAssociated) {
        if (isAssociated) {
            mContext.getContentResolver().insert(uri, sound.buildContentValues());
        } else {
            // TODO: this won't work for playlists
            mContext.getContentResolver().delete(uri, "item_id = ?", new String[]{
                String.valueOf(sound.id),
            });
        }
    }
}
