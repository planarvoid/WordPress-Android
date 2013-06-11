package com.soundcloud.android.cache;

import com.google.common.collect.ImmutableSet;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.ResolverHelper;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.SyncStateManager;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * I am deprecating this. It should be gone once we finish migrating from ScListView (god willing) and have full RX
 * support in our list loading.
 */
@Deprecated
public class FollowStatus {
    private final Set<Long> followings = Collections.synchronizedSet(new HashSet<Long>());
    private static FollowStatus sInstance;

    private WeakHashMap<Listener, Listener> listeners = new WeakHashMap<Listener, Listener>();
    private AsyncQueryHandler asyncQueryHandler;
    private Context mContext;
    private long last_sync_success = -1;
    private LocalCollection mFollowingCollectionState;

    private HashMap<Long,Long> followedAtStamps = new HashMap<Long, Long>();
    private HashMap<Long,Long> unFollowedAtStamps = new HashMap<Long, Long>();

    private SyncStateManager mSyncStateManager;

    protected FollowStatus(final Context context) {
        mContext = context;
        mSyncStateManager = new SyncStateManager();

        mFollowingCollectionState = mSyncStateManager.fromContent(Content.ME_FOLLOWINGS);
        mSyncStateManager.addChangeListener(mFollowingCollectionState, new LocalCollection.OnChangeListener() {
            @Override
            public void onLocalCollectionChanged(LocalCollection localCollection) {
                mFollowingCollectionState = localCollection;
                // if last sync has changed, do a new query
                if (mFollowingCollectionState.last_sync_success != last_sync_success) {
                    last_sync_success = mFollowingCollectionState.last_sync_success;
                    doQuery();
                }
            }
        });
    }

    public synchronized static FollowStatus get() {
        if (sInstance == null) {
            sInstance = new FollowStatus(SoundCloudApplication.instance);
        }
        return sInstance;
    }

    public synchronized static void clearState(){
        if (sInstance != null){
            sInstance.stopListening();
            sInstance = null;
        }
    }

    public void stopListening(){
        mSyncStateManager.removeChangeListener(mFollowingCollectionState);
    }

    public boolean isFollowing(long id) {
        return followings.contains(id);
    }

    public boolean isFollowing(User user) {
        return user != null && isFollowing(user.getId());
    }

    public synchronized void requestUserFollowings(final Listener listener) {
        // add this listener with a weak reference
        listeners.put(listener, null);
        if (asyncQueryHandler == null) {
            doQuery();
        }
    }

    public Set<Long> getFollowedUserIds() {
        return ImmutableSet.copyOf(followings);
    }

    public void toggleFollowing(List<SuggestedUser> suggestedUserList) {
        User[] toggleUsers = new User[suggestedUserList.size()];
        for (int i = 0; i < suggestedUserList.size(); i++) {
            toggleUsers[i] = new User(suggestedUserList.get(i));
        }
        toggleFollowing(toggleUsers);
    }

    private void doQuery(){
        asyncQueryHandler = new FollowingQueryHandler(mContext);
        asyncQueryHandler.startQuery(0, null, ResolverHelper.addIdOnlyParameter(Content.ME_FOLLOWINGS.uri),
                null, DBHelper.UserAssociations.REMOVED_AT + " IS NULL", null, null);
    }

    public void toggleFollowing(User... users){
        final boolean hadNoFollowings = followings.isEmpty();
        final List<User> toAdd = new ArrayList<User>();
        final List<User> toRemove = new ArrayList<User>();
        synchronized (followings) {
            for (User user : users){
                final boolean isNowFollowing = followings.contains(user.getId());
                if (isNowFollowing) {
                    toRemove.add(user);
                    followings.remove(user.getId());
                } else {
                    toAdd.add(user);
                    followings.add(user.getId());
                }
                // make sure the cache reflects the new state
                SoundCloudApplication.MODEL_MANAGER.cache(user, ScResource.CacheUpdateMode.NONE).user_following = isNowFollowing;
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // first follower, set the stream to stale so next time the users goes there it will sync
                if (hadNoFollowings && !toAdd.isEmpty()) mSyncStateManager.forceToStale(Content.ME_SOUND_STREAM);
                new UserAssociationStorage().addFollowings(toAdd);
                new UserAssociationStorage().removeFollowings(toRemove);
            }
        }).run();

        for (Listener l : listeners.keySet()) {
            l.onFollowChanged();
        }
    }

    public interface Listener {
        void onFollowChanged();
    }

    private class FollowingQueryHandler extends AsyncQueryHandler {
        public FollowingQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {

                followings.clear();
                while (cursor.moveToNext()) {
                    followings.add(cursor.getLong(0));
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
                    l.onFollowChanged();
                }
            }
        }
    }

}
