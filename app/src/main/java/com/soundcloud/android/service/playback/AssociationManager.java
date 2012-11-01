package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.AddAssociationTask;
import com.soundcloud.android.task.AssociatedTrackTask;
import com.soundcloud.android.task.RemoveAssociationTask;
import com.soundcloud.api.Endpoints;

import android.content.Context;
import android.content.Intent;

public class AssociationManager {

    private Context mContext;
    private ScModelManager mModelManager;

    public AssociationManager(Context context) {
        mContext = context;
        mModelManager = SoundCloudApplication.MODEL_MANAGER;
    }

    void addLike(Track track) {
        if (track == null) return;
        onLikeStatusSet(track, true);
        AddAssociationTask addAssociationTask = new AddAssociationTask(getApp(), track);
        addAssociationTask.setOnAssociatedListener(new AssociatedTrackTask.AssociatedListener() {
            @Override
            public void onNewStatus(Track track, boolean isAssociated) {
                onLikeStatusSet(track, isAssociated);
            }
        });
        addAssociationTask.execute(Endpoints.MY_FAVORITE);
    }


    void removeLike(Track track) {
        if (track == null) return;

        onLikeStatusSet(track, false);
        RemoveAssociationTask removeAssociationTask = new RemoveAssociationTask(getApp(), track);
        removeAssociationTask.setOnAssociatedListener(new AssociatedTrackTask.AssociatedListener() {
            @Override
            public void onNewStatus(Track track, boolean isAssociated) {
                onLikeStatusSet(track, isAssociated);
                LocalCollection.forceToStale(Content.ME_LIKES.uri, mContext.getContentResolver());

            }
        });
        removeAssociationTask.execute(Endpoints.MY_FAVORITE);
    }

    private void onLikeStatusSet(Track track, boolean isLike) {
        track.user_like = isLike;
        onAssociationChanged(track);
    }

    void addRepost(Track track) {
        if (track == null) return;
        onRepostStatusSet(track, true);
        AddAssociationTask addAssociationTask = new AddAssociationTask(getApp(), track);
        addAssociationTask.setOnAssociatedListener(new AssociatedTrackTask.AssociatedListener() {
            @Override
            public void onNewStatus(Track track, boolean isAssociated) {
                onRepostStatusSet(track, isAssociated);
            }
        });
        addAssociationTask.execute(TempEndpoints.e1.MY_REPOST);
    }

    void removeRepost(Track track) {
        if (track == null) return;
        onRepostStatusSet(track, false);
        RemoveAssociationTask removeAssociationTask = new RemoveAssociationTask(getApp(), track);
        removeAssociationTask.setOnAssociatedListener(new AssociatedTrackTask.AssociatedListener() {
            @Override
            public void onNewStatus(Track track, boolean isAssociated) {
                onRepostStatusSet(track, isAssociated);
            }
        });
        removeAssociationTask.execute(TempEndpoints.e1.MY_REPOST);
    }

    private void onRepostStatusSet(Track track, boolean isRepost) {
        track.user_repost = isRepost;
        LocalCollection.forceToStale(Content.ME_REPOSTS.uri, mContext.getContentResolver());
        onAssociationChanged(track);
    }

    private void onAssociationChanged(Track track) {

        mModelManager.cache(track, ScResource.CacheUpdateMode.NONE);

        Intent intent = new Intent(CloudPlaybackService.TRACK_ASSOCIATION_CHANGED)
                .putExtra(CloudPlaybackService.BroadcastExtras.id, track.id)
                .putExtra(CloudPlaybackService.BroadcastExtras.isRepost, track.user_repost)
                .putExtra(CloudPlaybackService.BroadcastExtras.isLike, track.user_like);

        mContext.sendBroadcast(intent);
        PlayerAppWidgetProvider.getInstance().notifyChange(mContext, intent);
    }

    private SoundCloudApplication getApp() {
        return SoundCloudApplication.fromContext(mContext);
    }

}
