package com.soundcloud.android.task;

import android.nfc.Tag;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Request;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class LoadFollowingsTask extends LoadJsonTask<Long> {

    private SoundCloudApplication mApp;
    private List<WeakReference<FollowingsListener>> mListeners;

    private static final int MAX_PAGE_SIZE = 5000;

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
        if (result != null){
            mApp.followingsSet = new HashSet<Long>();
            for (Long following : result){
                mApp.followingsSet.add(following);
            }
        } else {
            // fall back to last set on exception
            mApp.followingsSet = mApp.lastFollowingsSet;
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