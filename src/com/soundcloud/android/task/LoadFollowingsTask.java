package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Request;

import android.os.Parcelable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoadFollowingsTask extends LoadJsonTask<User> {

    private SoundCloudApplication mApp;
    private List<WeakReference<FollowingsListener>> mListeners;

    private static final int MAX_PAGE_SIZE = 50;

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
    protected List<User> doInBackground(Request... path) {
        List<User> followingsPage = null;
        List<User> followingsList = new ArrayList<User>();
        do{
            followingsPage = list(path[0], User.class);
            publishProgress((Parcelable[])followingsPage.toArray());
            followingsList.addAll(followingsPage);

        } while (followingsPage != null && followingsPage.size() == MAX_PAGE_SIZE);
        return followingsList;
    }

    @Override
    protected void onPreExecute() {
        mApp.followings = new ArrayList<User>();
    }

    @Override
    protected void onProgressUpdate(Parcelable... updates) {

        for (Parcelable user : updates){
            mApp.followings.add((User)user);
        }

        for (WeakReference<FollowingsListener> listenerRef : mListeners){
            if (listenerRef.get() != null) {
                listenerRef.get().onFollowingsPage((User[])updates);
            }
        }
    }


    @Override
    public void onPostExecute(List<User> result){
        super.onPostExecute(result);

        mApp.followingsMap = new HashMap<Long,Boolean>();
        if (result != null)
        for (User following : result){
            mApp.followingsMap.put(following.id, true);
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
        public abstract void onFollowingsPage(User[] followings);
    }
}
