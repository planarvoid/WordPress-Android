package com.soundcloud.android.model;

import com.google.common.primitives.Ints;
import com.soundcloud.android.cache.ModelCache;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

@Deprecated
public class ScModelManager {
    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1024 * 1024; // Available mem in bytes
    private static final int LOW_MEM_REFERENCE = 16 * 1024 * 1024; // In bytes, used as a reference for calculations
    private static final int DEFAULT_CACHE_CAPACITY = 100;

    private ContentResolver mResolver;

    private final ModelCache<Track> mTrackCache;
    private final ModelCache<User> mUserCache;
    private final ModelCache<Playlist> mPlaylistCache;


    public ScModelManager(Context c) {
        final long availableMemory = Runtime.getRuntime().maxMemory();
        final int trackCapacity;
        final int userCapacity;
        final int playlistCapacity;
        if(availableMemory < LOW_MEM_DEVICE_THRESHOLD) {
            trackCapacity = Ints.saturatedCast((availableMemory * 10) / LOW_MEM_REFERENCE);
            userCapacity = Ints.saturatedCast((availableMemory * 20) / LOW_MEM_REFERENCE);
            playlistCapacity = Ints.saturatedCast((availableMemory * 10) / LOW_MEM_REFERENCE);
        } else {
            trackCapacity =  DEFAULT_CACHE_CAPACITY * 4;
            userCapacity = DEFAULT_CACHE_CAPACITY * 2;
            playlistCapacity = DEFAULT_CACHE_CAPACITY;
        }

        mTrackCache = new ModelCache<Track>(trackCapacity);
        mUserCache = new ModelCache<User>(userCapacity);
        mPlaylistCache = new ModelCache<Playlist>(playlistCapacity);

        mResolver = c.getContentResolver();
    }

    public Track getCachedTrackFromCursor(Cursor cursor) {
        return getCachedTrackFromCursor(cursor, DBHelper.Sounds._ID);
    }

    public Track getCachedTrackFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Track track = mTrackCache.get(id);

        // assumes track cache has always
        if (track == null) {
            track = new Track(cursor);
            mTrackCache.put(track);
        }
        return track;
    }

    public Playlist getCachedPlaylistFromCursor(Cursor cursor) {
        return getCachedPlaylistFromCursor(cursor, DBHelper.Sounds._ID);
    }

    public Playlist getCachedPlaylistFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Playlist playlist = mPlaylistCache.get(id);

        // assumes track cache has always
        if (playlist == null) {
            playlist = new Playlist(cursor);
            mPlaylistCache.put(playlist);
        }
        return playlist;
    }

    public User getCachedUserFromSoundViewCursor(Cursor cursor) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(DBHelper.SoundView.USER_ID));
        User user = mUserCache.get(user_id);

        if (user == null) {
            user = User.fromSoundView(cursor);
            mUserCache.put(user);
        }
        return user;
    }

    public User getCachedUserFromCursor(Cursor cursor) {
        return getCachedUserFromCursor(cursor,DBHelper.Users._ID);
    }

    public User getCachedUserFromCursor(Cursor cursor, String col) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(col));
        User user = mUserCache.get(user_id);

        if (user == null) {
            user = new User(cursor);
            mUserCache.put(user);
        }
        return user;
    }

    public User getCachedUserFromActivityCursor(Cursor itemsCursor) {
        final long id = itemsCursor.getLong(itemsCursor.getColumnIndex(DBHelper.ActivityView.USER_ID));
        User user = mUserCache.get(id);
        if (user == null) {
            user = User.fromActivityView(itemsCursor);
            mUserCache.put(user);
        }
        return user;
    }

    public
    @Nullable
    Playlist getPlaylist(Uri uri) {
        return (Playlist) getModel(uri);
    }

    public void removeFromCache(Uri uri) {
        final ModelCache cacheFromUri = getCacheFromUri(uri);
        if (cacheFromUri != null) {
            cacheFromUri.remove(UriUtils.getLastSegmentAsLong(uri));
        }
    }

    private @Nullable ModelCache getCacheFromUri(Uri uri) {
        switch (Content.match(uri)) {
            case TRACK:
                return mTrackCache;
            case USER:
                return mUserCache;
            case PLAYLIST:
                return mPlaylistCache;
        }
        return null;
    }

    public
    @Nullable
    ScModel getModel(Uri uri) {
        return getModel(uri, getCacheFromUri(uri));
    }

    /**
     * Gets a resource from local storage, optionally from a cache if one is provided
     *
     * @param uri   resource lookup uri {@link Content}
     * @param cache optional cache to lookup object in and cache to
     * @return the resource found, or null if no resource found
     */
    private @Nullable ScModel getModel(Uri uri, @Nullable ModelCache cache) {
        ScModel resource = null;

        if (cache != null) resource = cache.get(UriUtils.getLastSegmentAsLong(uri));

        Content c = Content.match(uri);
        if (resource == null) {
            Cursor cursor = mResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    try {
                        resource = c.modelType.getConstructor(Cursor.class).newInstance(cursor);
                    } catch (Exception e) {
                        throw new AssertionError("Could not find constructor for resource. Uri: " + uri);
                    }
                }
                cursor.close();
            }
            if (cache != null && resource != null) {
                cache.put(resource);
            }
        }
        return resource;
    }

    public
    @Nullable
    Track getTrack(long id) {
        if (id < 0) return null;

        Track t = mTrackCache.get(id);
        if (t == null) {
            t = (Track) getModel(Content.TRACK.forId(id), null);
            if (t != null) mTrackCache.put(t);
        }
        return t;
    }

    public
    @Nullable
    User getUser(long id) {
        if (id < 0) return null;

        User u = mUserCache.get(id);
        if (u == null) {
            u = (User) getModel(Content.USER.forId(id));
            if (u != null) mUserCache.put(u);
        }
        return u;
    }


    public
    @Nullable
    Playlist getPlaylist(long id) {
        if (id < 0) return null;

        Playlist p = mPlaylistCache.get(id);
        if (p == null) {
            p = (Playlist) getModel(Content.PLAYLIST.forId(id));
            if (p != null) mPlaylistCache.put(p);
        }
        return p;
    }


    public Track getCachedTrack(long id) {
        return mTrackCache.get(id);
    }

    public User getCachedUser(long id) {
        return mUserCache.get(id);
    }

    public ScResource cache(@Nullable ScResource resource) {
        return cache(resource, ScResource.CacheUpdateMode.NONE);
    }

    public ScResource cache(@Nullable ScResource resource, ScResource.CacheUpdateMode updateMode) {
        if (resource instanceof Track) {
            return cache((Track) resource, updateMode);
        } else if (resource instanceof Playlist) {
            return cache((Playlist) resource, updateMode);
        } else if (resource instanceof User) {
            return cache((User) resource, updateMode);
        } else {
            return resource;
        }
    }

    public Track cache(@Nullable Track track) {
        return cache(track, ScResource.CacheUpdateMode.NONE);
    }

    public Playlist cache(@Nullable Playlist playlist) {
        return cache(playlist, ScResource.CacheUpdateMode.NONE);
    }

    public Track cache(@Nullable Track track, ScResource.CacheUpdateMode updateMode) {
        if (track == null) return null;

        if (track.user != null) {
            track.user = cache(track.user, updateMode);
        }

        if (mTrackCache.containsKey(track.getId())) {
            if (updateMode.shouldUpdate()) {
                return mTrackCache.get(track.getId()).updateFrom(track, updateMode);
            } else {
                return mTrackCache.get(track.getId());
            }

        } else {
            mTrackCache.put(track);
            return track;
        }
    }

    public Playlist cache(@Nullable Playlist playlist, ScResource.CacheUpdateMode updateMode) {
        if (playlist == null) return null;

        if (playlist.user != null) {
            playlist.user = cache(playlist.user, updateMode);
        }

        for (int i = 0; i < playlist.tracks.size(); i++) {
            playlist.tracks.set(i, cache(playlist.tracks.get(i), updateMode));
        }

        if (mPlaylistCache.containsKey(playlist.getId())) {
            if (updateMode.shouldUpdate()) {
                return mPlaylistCache.get(playlist.getId()).updateFrom(playlist, updateMode);
            } else {
                return mPlaylistCache.get(playlist.getId());
            }
        } else {
            mPlaylistCache.put(playlist);
            return playlist;
        }
    }

    public ScResource cache(@Nullable User user) {
        return cache(user, ScResource.CacheUpdateMode.NONE);
    }

    public User cache(@Nullable User user, ScResource.CacheUpdateMode updateMode) {
        if (user == null) return null;

        if (mUserCache.containsKey(user.getId())) {
            if (updateMode.shouldUpdate()) {
                return mUserCache.get(user.getId()).updateFrom(user, updateMode);
            } else {
                return mUserCache.get(user.getId());
            }
        } else {
            mUserCache.put(user);
            return user;
        }
    }

    public void clear() {
        mTrackCache.clear();
        mUserCache.clear();
    }
}
