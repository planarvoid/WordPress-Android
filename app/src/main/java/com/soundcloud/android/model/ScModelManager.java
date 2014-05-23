package com.soundcloud.android.model;

import com.google.common.primitives.Ints;
import com.soundcloud.android.cache.ModelCache;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import javax.inject.Singleton;

@Deprecated
@Singleton
public class ScModelManager {
    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1024 * 1024; // Available mem in bytes
    private static final int LOW_MEM_REFERENCE = 16 * 1024 * 1024; // In bytes, used as a reference for calculations
    private static final int DEFAULT_CACHE_CAPACITY = 100;

    private ContentResolver resolver;

    private final ModelCache<Track> trackCache;
    private final ModelCache<User> userCache;
    private final ModelCache<Playlist> playlistCache;


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

        trackCache = new ModelCache<Track>(trackCapacity);
        userCache = new ModelCache<User>(userCapacity);
        playlistCache = new ModelCache<Playlist>(playlistCapacity);

        resolver = c.getContentResolver();
    }

    public Track getCachedTrackFromCursor(Cursor cursor) {
        return getCachedTrackFromCursor(cursor, TableColumns.Sounds._ID);
    }

    public Track getCachedTrackFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Track track = trackCache.get(id);

        // assumes track cache has always
        if (track == null) {
            track = new Track(cursor);
            trackCache.put(track);
        }
        return track;
    }

    public Playlist getCachedPlaylistFromCursor(Cursor cursor) {
        return getCachedPlaylistFromCursor(cursor, TableColumns.Sounds._ID);
    }

    public Playlist getCachedPlaylistFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        Playlist playlist = playlistCache.get(id);

        // assumes track cache has always
        if (playlist == null) {
            playlist = new Playlist(cursor);
            playlistCache.put(playlist);
        }
        return playlist;
    }

    public User getCachedUserFromSoundViewCursor(Cursor cursor) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(TableColumns.SoundView.USER_ID));
        User user = userCache.get(user_id);

        if (user == null) {
            user = User.fromSoundView(cursor);
            userCache.put(user);
        }
        return user;
    }

    public User getCachedUserFromCursor(Cursor cursor) {
        return getCachedUserFromCursor(cursor, TableColumns.Users._ID);
    }

    public User getCachedUserFromCursor(Cursor cursor, String col) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(col));
        User user = userCache.get(user_id);

        if (user == null) {
            user = new User(cursor);
            userCache.put(user);
        }
        return user;
    }

    public User getCachedUserFromActivityCursor(Cursor itemsCursor) {
        final long id = itemsCursor.getLong(itemsCursor.getColumnIndex(TableColumns.ActivityView.USER_ID));
        User user = userCache.get(id);
        if (user == null) {
            user = User.fromActivityView(itemsCursor);
            userCache.put(user);
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
                return trackCache;
            case USER:
                return userCache;
            case PLAYLIST:
                return playlistCache;
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
            Cursor cursor = resolver.query(uri, null, null, null, null);
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

        Track t = trackCache.get(id);
        if (t == null) {
            t = (Track) getModel(Content.TRACK.forId(id), null);
            if (t != null) trackCache.put(t);
        }
        return t;
    }

    public
    @Nullable
    User getUser(long id) {
        if (id < 0) return null;

        User u = userCache.get(id);
        if (u == null) {
            u = (User) getModel(Content.USER.forId(id));
            if (u != null) userCache.put(u);
        }
        return u;
    }


    public
    @Nullable
    Playlist getPlaylist(long id) {
        if (id < 0) return null;

        Playlist p = playlistCache.get(id);
        if (p == null) {
            p = (Playlist) getModel(Content.PLAYLIST.forId(id));
            if (p != null) playlistCache.put(p);
        }
        return p;
    }


    public Track getCachedTrack(long id) {
        return trackCache.get(id);
    }

    public User getCachedUser(long id) {
        return userCache.get(id);
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

        if (trackCache.containsKey(track.getId())) {
            if (updateMode.shouldUpdate()) {
                return trackCache.get(track.getId()).updateFrom(track, updateMode);
            } else {
                return trackCache.get(track.getId());
            }

        } else {
            trackCache.put(track);
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

        if (playlistCache.containsKey(playlist.getId())) {
            if (updateMode.shouldUpdate()) {
                return playlistCache.get(playlist.getId()).updateFrom(playlist, updateMode);
            } else {
                return playlistCache.get(playlist.getId());
            }
        } else {
            playlistCache.put(playlist);
            return playlist;
        }
    }

    public ScResource cache(@Nullable User user) {
        return cache(user, ScResource.CacheUpdateMode.NONE);
    }

    public User cache(@Nullable User user, ScResource.CacheUpdateMode updateMode) {
        if (user == null) return null;

        if (userCache.containsKey(user.getId())) {
            if (updateMode.shouldUpdate()) {
                return userCache.get(user.getId()).updateFrom(user, updateMode);
            } else {
                return userCache.get(user.getId());
            }
        } else {
            userCache.put(user);
            return user;
        }
    }

    public void clear() {
        trackCache.clear();
        userCache.clear();
    }
}
