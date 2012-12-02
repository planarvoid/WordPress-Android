package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class FollowStatus {
    public static  final int FOLLOW_STATUS_SUCCESS = 0;
    public static  final int FOLLOW_STATUS_FAIL    = 1;
    public static  final int FOLLOW_STATUS_SPAM    = 2;

    private final Set<Long> followings = Collections.synchronizedSet(new HashSet<Long>());
    private static FollowStatus sInstance;

    private WeakHashMap<Listener, Listener> listeners = new WeakHashMap<Listener, Listener>();
    private AsyncQueryHandler asyncQueryHandler;
    private Context mContext;

    protected FollowStatus(final Context c) {
        mContext = c;
        c.getContentResolver().registerContentObserver(Content.ME_FOLLOWINGS.uri,true,
                new ContentObserver(new Handler()) {
                    @Override
                    public boolean deliverSelfNotifications() {
                        return true;
                    }

                    @Override
                    public void onChange(boolean selfChange) {
                        doQuery();
                    }
        });
    }

    public synchronized static FollowStatus get(Context c) {
        if (sInstance == null) {
            sInstance = new FollowStatus(c);
        }
        return sInstance;
    }

    public synchronized static void set(FollowStatus status) {
        sInstance = status;
    }

    public boolean isFollowing(long id) {
        return followings.contains(id);
    }

    public boolean isFollowing(User user) {
        return user != null && isFollowing(user.id);
    }

    public synchronized void requestUserFollowings(final Listener listener) {
        // add this listener with a weak reference
        listeners.put(listener, null);
        if (asyncQueryHandler == null) {
            doQuery();
        }
    }

    private void doQuery(){
        asyncQueryHandler = new FollowingQueryHandler(mContext);
        asyncQueryHandler.startQuery(0, null, Content.ME_FOLLOWINGS.uri, new String[]{DBHelper.CollectionItems.ITEM_ID}, null, null, null);
    }


    public AsyncTask<User,Void,Boolean> toggleFollowing(final User user,
                                final SoundCloudApplication app,
                                final Handler handler) {
        final boolean addFollowing = toggleFollowing(user.id);

        return new AsyncApiTask<User,Void,Boolean>(app) {

            int status;

            @Override
            protected Boolean doInBackground(User... params) {

                User u = params[0];
                final Request request = Request.to(Endpoints.MY_FOLLOWING, u.id);
                try {

                    status = (addFollowing ? app.put(request) : app.delete(request)).getStatusLine().getStatusCode();
                    final boolean success;
                    if (addFollowing) {
                        // new following or already following
                        success = status == HttpStatus.SC_CREATED || status == HttpStatus.SC_OK;
                    } else {
                        success = status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_FOUND;
                    }
                    if (!success) {
                        Log.w(TAG, "error changing following status, resp=" + status);
                    }
                    return success;

                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    // make sure the cache reflects the new state
                    SoundCloudApplication.MODEL_MANAGER.cache(user, ScResource.CacheUpdateMode.NONE).user_following = addFollowing;

                    // tell the list to refresh itself next time
                    LocalCollection.forceToStale(Content.ME_FOLLOWINGS.uri, mContext.getContentResolver());
                    for (Listener l : listeners.keySet()) {
                        l.onFollowChanged(true);
                    }
                    if (handler != null) {
                        Message.obtain(handler, FOLLOW_STATUS_SUCCESS).sendToTarget();
                    }
                } else {
                    updateFollowing(user.id, !addFollowing); // revert state change

                    if (handler != null) {
                        Message.obtain(handler, status == 429 ? FOLLOW_STATUS_SPAM : FOLLOW_STATUS_FAIL).sendToTarget();
                    }
                }
            }
        }.execute(user);
    }

  /* package */ void updateFollowing(long userId, boolean follow) {
        if (follow) {
            followings.add(userId);
        } else {
            followings.remove(userId);
        }
    }

    /* package */ boolean toggleFollowing(long userId) {
        synchronized (followings) {
            if (followings.contains(userId)) {
                followings.remove(userId);
                return false;
            } else {
                followings.add(userId);
                return true;
            }
        }
    }


    public interface Listener {
        void onFollowChanged(boolean success);
    }

    private class FollowingQueryHandler extends AsyncQueryHandler {
        public FollowingQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                followings.clear();
                if (cursor.moveToFirst()) {
                    do {
                        followings.add(cursor.getLong(0));
                    } while (cursor.moveToNext());
                }
                cursor.close();

                for (Listener l : listeners.keySet()) {
                    l.onFollowChanged(true);
                }
            }
        }
    }

}
