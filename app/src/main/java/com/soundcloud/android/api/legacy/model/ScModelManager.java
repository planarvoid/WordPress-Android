package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.java.primitives.Ints;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import javax.inject.Singleton;

@SuppressWarnings("deprecation")
@Deprecated
@Singleton
public class ScModelManager {
    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1024 * 1024; // Available mem in bytes
    private static final int LOW_MEM_REFERENCE = 16 * 1024 * 1024; // In bytes, used as a reference for calculations
    private static final int DEFAULT_CACHE_CAPACITY = 100;

    private final ContentResolver resolver;

    private final ModelCache<PublicApiTrack> trackCache;
    private final ModelCache<PublicApiUser> userCache;
    private final ModelCache<PublicApiPlaylist> playlistCache;


    public ScModelManager(Context c) {
        final long availableMemory = Runtime.getRuntime().maxMemory();
        final int trackCapacity;
        final int userCapacity;
        final int playlistCapacity;
        if (availableMemory < LOW_MEM_DEVICE_THRESHOLD) {
            trackCapacity = Ints.saturatedCast((availableMemory * 10) / LOW_MEM_REFERENCE);
            userCapacity = Ints.saturatedCast((availableMemory * 10) / LOW_MEM_REFERENCE);
            playlistCapacity = Ints.saturatedCast((availableMemory * 10) / LOW_MEM_REFERENCE);
        } else {
            trackCapacity = DEFAULT_CACHE_CAPACITY;
            userCapacity = DEFAULT_CACHE_CAPACITY;
            playlistCapacity = DEFAULT_CACHE_CAPACITY;
        }

        trackCache = new ModelCache<>(trackCapacity);
        userCache = new ModelCache<>(userCapacity);
        playlistCache = new ModelCache<>(playlistCapacity);

        resolver = c.getContentResolver();
    }

    public PublicApiTrack getCachedTrackFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        PublicApiTrack track = trackCache.get(id);

        // assumes track cache has always
        if (track == null) {
            track = new PublicApiTrack(cursor);
            trackCache.put(track);
        }
        return track;
    }

    public PublicApiPlaylist getCachedPlaylistFromCursor(Cursor cursor, String idCol) {
        final long id = cursor.getLong(cursor.getColumnIndex(idCol));
        PublicApiPlaylist playlist = playlistCache.get(id);

        // assumes track cache has always
        if (playlist == null) {
            playlist = new PublicApiPlaylist(cursor);
            playlistCache.put(playlist);
        }
        return playlist;
    }

    public PublicApiUser getCachedUserFromSoundViewCursor(Cursor cursor) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(TableColumns.SoundView.USER_ID));
        PublicApiUser user = userCache.get(user_id);

        if (user == null) {
            user = PublicApiUser.fromSoundView(cursor);
            userCache.put(user);
        }
        return user;
    }

    public PublicApiUser getCachedUserFromCursor(Cursor cursor, String col) {
        final long user_id = cursor.getLong(cursor.getColumnIndex(col));
        PublicApiUser user = userCache.get(user_id);

        if (user == null) {
            user = new PublicApiUser(cursor);
            userCache.put(user);
        }
        return user;
    }

    public PublicApiUser getCachedUserFromActivityCursor(Cursor itemsCursor) {
        final long id = itemsCursor.getLong(itemsCursor.getColumnIndex(TableColumns.ActivityView.USER_ID));
        PublicApiUser user = userCache.get(id);
        if (user == null) {
            user = PublicApiUser.fromActivityView(itemsCursor);
            userCache.put(user);
        }
        return user;
    }

    private
    @Nullable
    ModelCache getCacheFromUri(Uri uri) {
        switch (Content.match(uri)) {
            case TRACK:
                return trackCache;
            case USER:
                return userCache;
            case PLAYLIST:
                return playlistCache;
            default:
                return null;
        }
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
    private
    @Nullable
    ScModel getModel(Uri uri, @Nullable ModelCache cache) {
        ScModel resource = null;

        if (cache != null) {
            resource = cache.get(UriUtils.getLastSegmentAsLong(uri));
        }

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
    PublicApiTrack getTrack(long id) {
        if (id < 0) {
            return null;
        }

        PublicApiTrack t = trackCache.get(id);
        if (t == null) {
            t = (PublicApiTrack) getModel(Content.TRACK.forId(id), null);
            if (t != null) {
                trackCache.put(t);
            }
        }
        return t;
    }

    public
    @Nullable
    PublicApiUser getUser(long id) {
        if (id < 0) {
            return null;
        }

        PublicApiUser u = userCache.get(id);
        if (u == null) {
            u = (PublicApiUser) getModel(Content.USER.forId(id));
            if (u != null) {
                userCache.put(u);
            }
        }
        return u;
    }


    public PublicApiUser getCachedUser(long id) {
        return userCache.get(id);
    }

    public PublicApiResource cache(@Nullable PublicApiResource resource) {
        return cache(resource, PublicApiResource.CacheUpdateMode.NONE);
    }

    public PublicApiResource cache(@Nullable PublicApiResource resource, PublicApiResource.CacheUpdateMode updateMode) {
        if (resource instanceof PublicApiTrack) {
            return cache((PublicApiTrack) resource, updateMode);
        } else if (resource instanceof PublicApiPlaylist) {
            return cache((PublicApiPlaylist) resource, updateMode);
        } else if (resource instanceof PublicApiUser) {
            return cache((PublicApiUser) resource, updateMode);
        } else {
            return resource;
        }
    }

    public PublicApiTrack cache(@Nullable PublicApiTrack track) {
        return cache(track, PublicApiResource.CacheUpdateMode.NONE);
    }

    public PublicApiPlaylist cache(@Nullable PublicApiPlaylist playlist) {
        return cache(playlist, PublicApiResource.CacheUpdateMode.NONE);
    }

    public PublicApiTrack cache(@Nullable PublicApiTrack track, PublicApiResource.CacheUpdateMode updateMode) {
        if (track == null) {
            return null;
        }

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

    public PublicApiPlaylist cache(@Nullable PublicApiPlaylist playlist, PublicApiResource.CacheUpdateMode updateMode) {
        if (playlist == null) {
            return null;
        }

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

    public PublicApiResource cache(@Nullable PublicApiUser user) {
        return cache(user, PublicApiResource.CacheUpdateMode.NONE);
    }

    public PublicApiUser cache(@Nullable PublicApiUser user, PublicApiResource.CacheUpdateMode updateMode) {
        if (user == null) {
            return null;
        }

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
