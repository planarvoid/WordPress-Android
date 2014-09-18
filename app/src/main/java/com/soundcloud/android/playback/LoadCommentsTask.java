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

    private List<WeakReference<LoadCommentsListener>> mListenerRefs;
    private long mTrackId;

    public LoadCommentsTask(PublicCloudAPI api) {
        super(api);
        mListenerRefs = new ArrayList<WeakReference<LoadCommentsListener>>();
    }

    public void addListener(LoadCommentsListener listener) {
        for (WeakReference<LoadCommentsListener> listenerRef : mListenerRefs) {
            if (listener != null && listenerRef.get() != null && listenerRef.get() == listener) {
                return;
            }
        }
        mListenerRefs.add(new WeakReference<LoadCommentsListener>(listener));
    }

    @Override
    protected List<PublicApiComment> doInBackground(Long... params) {
        mTrackId = params[0];
        return list(Request.to(Endpoints.TRACK_COMMENTS, mTrackId)
                .add("limit", Consts.MAX_COMMENTS_TO_LOAD));
    }

    @Override
    protected void onPostExecute(List<PublicApiComment> comments) {
        if (comments != null) {
            PublicApiTrack cached = SoundCloudApplication.sModelManager.getTrack(mTrackId);

            if (cached != null) {
                cached.comments = comments;
                for (PublicApiComment c : comments) {
                    c.track = cached;
                }
            }

            for (WeakReference<LoadCommentsListener> listenerRef : mListenerRefs) {
                if (listenerRef != null && listenerRef.get() != null) {
                    listenerRef.get().onCommentsLoaded(mTrackId, comments);
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
