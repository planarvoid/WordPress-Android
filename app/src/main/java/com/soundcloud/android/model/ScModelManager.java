package com.soundcloud.android.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.cache.LruCache;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.cache.UserCache;

import android.content.ContentResolver;
import android.content.Context;

public class ScModelManager {

    private Context mContext;
    private ContentResolver mResolver;
    private ObjectMapper mMapper;

    private TrackCache mTrackCache;
    private UserCache mUserCache;

    public ScModelManager(Context context, ObjectMapper mapper) {
        mContext = context;
        mResolver = context.getContentResolver();
        mMapper = mapper;

        mTrackCache = new TrackCache();
        mUserCache = new UserCache();
    }

    public ScResource getCachedModel(Class<? extends ScResource> modelClass, long id) {
        if (Track.class.equals(modelClass)) {
            return getCachedTrack(id);
        } else if (User.class.equals(modelClass)) {
            return getCachedUser(id);
        }
        return null;
    }

    public Track getCachedTrack(long id) {
        return mTrackCache.get(id);
    }

    public User getCachedUser(long id) {
        return mUserCache.get(id);
    }

    public ScResource cache(ScResource resource) {
        if (resource instanceof Track) {
            return cache((Track) resource);
        } else if (resource instanceof User) {
            return cache((User) resource);
        } else {
            return resource;
        }
    }

    public Track cache(Track model) {
        if (mTrackCache.containsKey(model.id)) {
            return mTrackCache.get(model.id).updateFrom(mContext, model);
        } else {
            mTrackCache.put(model);
            return model;
        }
    }

    public User cache(User model) {
        if (mUserCache.containsKey(model.id)) {
            return mUserCache.get(model.id).updateFrom(model);
        } else {
            mUserCache.put(model);
            return model;
        }

    }
}
