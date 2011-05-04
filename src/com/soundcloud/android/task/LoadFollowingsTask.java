package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.api.Request;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoadFollowingsTask extends LoadJsonTask<Long> {

    private SoundCloudApplication mApp;
    private List<WeakReference<FollowingsListener>> mListeners;

    public LoadFollowingsTask(SoundCloudApplication app) {
        super(app);
        mApp = app;
        mListeners = new ArrayList<WeakReference<FollowingsListener>>();
    }

    public void addListener(FollowingsListener listener){
        for (WeakReference<FollowingsListener> listenerRef : mListeners){
            if (listenerRef.get() != null && listenerRef.get() == listener) return;
        }
        mListeners.add(new WeakReference<FollowingsListener>(listener));
    }

    @Override
    protected List<Long> doInBackground(Request... path) {
        return list(path[0], Long.class);
    }


    @Override
    public void onPostExecute(List<Long> result){
        super.onPostExecute(result);

        mApp.followingsMap = new HashMap<Long,Boolean>();
        if (result != null)
        for (Long followingId : result){
            mApp.followingsMap.put(followingId, true);
        }

        for (WeakReference<FollowingsListener> listenerRef : mListeners){
            if (listenerRef.get() != null) {
                listenerRef.get().onFollowings((result != null));
            }
        }
    }

 // Define our custom Listener interface
    public interface FollowingsListener {
        public abstract void onFollowings(boolean success);
    }
}
