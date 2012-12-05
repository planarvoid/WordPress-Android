package com.soundcloud.android.cache;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
    private long last_sync_success = -1;
    private LocalCollection mFollowingCollectionState;

    private HashMap<Long,Long> followedAtStamps = new HashMap<Long, Long>();
    private HashMap<Long,Long> unFollowedAtStamps = new HashMap<Long, Long>();

    protected FollowStatus(final Context c) {
        mContext = c;

        mFollowingCollectionState = LocalCollection.fromContent(Content.ME_FOLLOWINGS,mContext.getContentResolver(),true);
        mFollowingCollectionState.startObservingSelf(mContext.getContentResolver(), new LocalCollection.OnChangeListener() {
            @Override
            public void onLocalCollectionChanged() {
                // if last sync has changed, do a new query
                if (mFollowingCollectionState.last_sync_success != last_sync_success) {
                    last_sync_success = mFollowingCollectionState.last_sync_success;
                    doQuery();
                }
            }
        });
    }

    public synchronized static FollowStatus get(Context c) {
        if (sInstance == null) {
            sInstance = new FollowStatus(c.getApplicationContext());
        }
        return sInstance;
    }

    public synchronized static void set(FollowStatus status) {
        sInstance = status;
    }

    public int getFollowingCount(){
        return followings.size();
    }

    public boolean isFollowing(long id) {
        return followings.contains(id);
    }

    public boolean isFollowing(User user) {
        return user != null && isFollowing(user.id);
    }

    public synchronized void requestUserFollowings(final Listener listener) {

        if (mFollowingCollectionState.shouldAutoRefresh()) {
            // sync users for proper following display
            Intent intent = new Intent(mContext, ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .setData(Content.ME_FOLLOWINGS.uri);
        }

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
                        followedAtStamps.put(u.id, System.currentTimeMillis());
                        unFollowedAtStamps.remove(u.id);

                        // new following or already following
                        success = status == HttpStatus.SC_CREATED || status == HttpStatus.SC_OK;
                    } else {
                        unFollowedAtStamps.put(u.id,System.currentTimeMillis());
                        followedAtStamps.remove(u.id);

                        success = status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_FOUND;
                    }
                    if (!success) {
                        Log.w(TAG, "error changing following status, resp=" + status);
                    } else {

                        // tell the list to refresh itself next time
                        LocalCollection.forceToStale(Content.ME_FOLLOWINGS.uri, mContext.getContentResolver());
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

                    if (followings.isEmpty() && addFollowing){
                        LocalCollection.forceToStale(Content.ME_SOUND_STREAM.uri, mContext.getContentResolver());
                    }

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

                // update with anything that has occurred since last sync

                for (Long id : followedAtStamps.keySet()){
                    if (followedAtStamps.get(id) > last_sync_success) followings.add(id);
                }

                for (Long id : unFollowedAtStamps.keySet()) {
                    if (unFollowedAtStamps.get(id) > last_sync_success) followings.remove(id);
                }

                for (Listener l : listeners.keySet()) {
                    l.onFollowChanged(true);
                }
            }
        }
    }

}
