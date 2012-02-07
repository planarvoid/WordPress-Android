package com.soundcloud.android.task;

import android.content.ContentResolver;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.model.User;

import java.lang.ref.WeakReference;

public class LoadUserInfoTask extends LoadTask<User> {
    private long mUserId;
    private WeakReference<LoadUserInfoListener> mListenerWeakReference;

    public LoadUserInfoTask(SoundCloudApplication app, long userId) {
        super(app, User.class);
        mUserId = userId;
    }

    public void setListener(LoadUserInfoListener listener){
        mListenerWeakReference = new WeakReference<LoadUserInfoListener>(listener);
    }

    @Override
    protected void onPostExecute(User result) {
        super.onPostExecute(result);

        LoadUserInfoListener listener = mListenerWeakReference != null ? mListenerWeakReference.get() : null;
        if (result != null) {
            if (listener != null) {
                listener.onUserInfoLoaded(result);
            }
        } else if (listener != null) {
            listener.onUserInfoError(mUserId);
        }
    }

    @Override
    protected void updateLocally(ContentResolver resolver, User user) {
        user.last_updated = System.currentTimeMillis();
        SoundCloudApplication.USER_CACHE.putWithLocalFields(user);
        SoundCloudDB.upsertUser(resolver, user);
    }

    // Define our custom Listener interface
    public interface LoadUserInfoListener {
        void onUserInfoLoaded(User user);
        void onUserInfoError(long userId);
    }
}
