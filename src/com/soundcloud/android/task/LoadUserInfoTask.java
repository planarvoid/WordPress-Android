package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import java.lang.ref.WeakReference;

public class LoadUserInfoTask extends LoadTask<User> {
    private SoundCloudApplication mApp;
    private boolean mWriteToDB;
    private long mUserId;
    private WeakReference<LoadUserInfoListener> mListenerWeakReference;

    public LoadUserInfoTask(SoundCloudApplication app, long userId, boolean cacheResult, boolean writeToDb) {
        super(app, User.class);
        mApp = app;
        mUserId = userId;
        mWriteToDB = writeToDb;
    }

    public void setListener(LoadUserInfoListener listener){
        mListenerWeakReference = new WeakReference<LoadUserInfoListener>(listener);
    }

    @Override
    protected void onPostExecute(User result) {
        super.onPostExecute(result);

        LoadUserInfoListener listener = mListenerWeakReference != null ? mListenerWeakReference.get() : null;
        if (result != null) {

            if (mWriteToDB){
                SoundCloudDB.writeUser(mApp.getContentResolver(), result,
                        WriteState.all, mApp.getCurrentUserId());
            }

            if (listener != null){
                listener.onUserInfoLoaded(result);
            }

        } else if (listener != null){
            listener.onUserInfoError(mUserId);
        }
    }

    // Define our custom Listener interface
    public interface LoadUserInfoListener {
        void onUserInfoLoaded(User user);
        void onUserInfoError(long trackId);
    }
}
