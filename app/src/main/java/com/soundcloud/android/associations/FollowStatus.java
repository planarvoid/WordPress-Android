package com.soundcloud.android.associations;

import static com.soundcloud.android.associations.FollowingOperations.FollowStatusChangedListener;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.sync.SyncStateManager;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * I am deprecating this. It should be gone once we finish migrating from ScListView (god willing) and have full RX
 * support in our list loading.
 */
@Deprecated
/* package */ class FollowStatus {
    private final Set<Long> followings = Collections.synchronizedSet(new HashSet<Long>());
    private static FollowStatus instance;

    private final WeakHashMap<FollowStatusChangedListener, FollowStatusChangedListener> listeners =
            new WeakHashMap<>();

    private AsyncQueryHandler asyncQueryHandler;
    private long last_sync_success = -1;
    private LocalCollection followingCollectionState;

    private final Context context;
    private final SyncStateManager syncStateManager;

    FollowStatus(final Context context, SyncStateManager syncStateManager) {
        this.context = context;
        this.syncStateManager = syncStateManager;

        followingCollectionState = syncStateManager.fromContent(Content.ME_FOLLOWINGS);
        syncStateManager.addChangeListener(followingCollectionState, new LocalCollection.OnChangeListener() {
            @Override
            public void onLocalCollectionChanged(LocalCollection localCollection) {
                followingCollectionState = localCollection;
                // if last sync has changed, do a new query
                if (followingCollectionState.last_sync_success != last_sync_success) {
                    last_sync_success = followingCollectionState.last_sync_success;
                    doQuery();
                }
            }
        });
    }

    synchronized static FollowStatus get() {
        if (instance == null) {
            final Context context = SoundCloudApplication.instance;
            instance = new FollowStatus(context, new SyncStateManager(context));
        }
        return instance;
    }

    public synchronized static void clearState() {
        if (instance != null) {
            instance.stopListening();
            instance = null;
        }
    }

    public void stopListening() {
        syncStateManager.removeChangeListener(followingCollectionState);
    }

    public boolean isFollowing(Urn urn) {
        return followings.contains(urn.getNumericId());
    }

    public boolean isFollowing(PublicApiUser user) {
        return user != null && isFollowing(user.getUrn());
    }

    public synchronized void requestUserFollowings(final FollowStatusChangedListener listener) {
        // add this listener with a weak reference
        listeners.put(listener, null);
        if (asyncQueryHandler == null) {
            doQuery();
        }
    }

    public Set<Long> getFollowedUserIds() {
        return Collections.unmodifiableSet(followings);
    }

    public boolean isEmpty() {
        return followings.isEmpty();
    }

    private void doQuery() {
        asyncQueryHandler = new FollowingQueryHandler(context);
        final Uri uri = Content.ME_FOLLOWINGS.uri.buildUpon()
                .appendQueryParameter(ScContentProvider.Parameter.IDS_ONLY, "1").build();
        asyncQueryHandler.startQuery(0, null, uri,
                null, TableColumns.UserAssociations.REMOVED_AT + " IS NULL", null, null);
    }

    /* package */ void addFollowing(long... userIds) {
        for (long userId : userIds) {
            followings.add(userId);
        }
        notifyOnFollowChanged();
    }

    /* package */ void removeFollowing(long... userIds) {
        for (long userId : userIds) {
            followings.remove(userId);
        }
        notifyOnFollowChanged();
    }

    private void notifyOnFollowChanged() {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (FollowStatusChangedListener l : listeners.keySet()) {
                    l.onFollowChanged();
                }
            }
        });
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

                notifyOnFollowChanged();
            }
        }
    }
}
