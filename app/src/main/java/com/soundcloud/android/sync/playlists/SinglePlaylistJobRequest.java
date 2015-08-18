package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SingleJobRequest;
import com.soundcloud.android.sync.SyncJob;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.likes.DefaultSyncJob;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.ResultReceiver;

public class SinglePlaylistJobRequest extends SingleJobRequest {

    private final Urn playlistUrn;

    public SinglePlaylistJobRequest(DefaultSyncJob syncJob, String action, boolean isHighPriority,
                                    ResultReceiver resultReceiver, EventBus eventBus, Urn playlistUrn) {
        super(syncJob, action, isHighPriority, resultReceiver, eventBus);
        this.playlistUrn = playlistUrn;
    }

    @Override
    public void processJobResult(SyncJob syncJob) {
        Exception exception = syncJob.getException();
        resultEvent = exception == null ?
                SyncResult.success(action, syncJob.resultedInAChange(), playlistUrn)
                : SyncResult.failure(action, exception);
    }
}
