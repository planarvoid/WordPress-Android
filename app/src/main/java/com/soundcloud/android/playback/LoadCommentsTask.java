package com.soundcloud.android.playback;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.AsyncApiTask;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class LoadCommentsTask extends AsyncApiTask<Long, Void, List<PublicApiComment>> {

    private final List<WeakReference<LoadCommentsListener>> listenerRefs;
    private long trackId;

    public LoadCommentsTask(PublicCloudAPI api) {
        super(api);
        listenerRefs = new ArrayList<>();
    }

    public void addListener(LoadCommentsListener listener) {
        for (WeakReference<LoadCommentsListener> listenerRef : listenerRefs) {
            if (listener != null && listenerRef.get() != null && listenerRef.get() == listener) {
                return;
            }
        }
        listenerRefs.add(new WeakReference<>(listener));
    }

    @Override
    protected List<PublicApiComment> doInBackground(Long... params) {
        trackId = params[0];
        return list(Request.to(Endpoints.TRACK_COMMENTS, trackId)
                .add("limit", Consts.MAX_COMMENTS_TO_LOAD));
    }

    @Override
    protected void onPostExecute(List<PublicApiComment> comments) {
        if (comments != null) {
            PublicApiTrack cached = SoundCloudApplication.sModelManager.getTrack(trackId);

            if (cached != null) {
                cached.comments = comments;
                for (PublicApiComment c : comments) {
                    c.track = cached;
                }
            }

            for (WeakReference<LoadCommentsListener> listenerRef : listenerRefs) {
                if (listenerRef != null && listenerRef.get() != null) {
                    listenerRef.get().onCommentsLoaded(trackId, comments);
                }
            }
        }
    }

    // Define our custom Listener interface
    public interface LoadCommentsListener {
        void onCommentsLoaded(long track_id, List<PublicApiComment> comments);
    }

    private List<PublicApiComment> list(Request path) {
        try {
            return api.readList(path);
        } catch (IOException e) {
            Log.w(TAG, "error fetching JSON", e);
            return null;
        }
    }
}
