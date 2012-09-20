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
    private LruCache<Long, User> mUserCache;

    public ScModelManager(Context context, ObjectMapper mapper) {
        mContext = context;
        mResolver = context.getContentResolver();
        mMapper = mapper;

        mTrackCache = new TrackCache();
        mUserCache = new UserCache();
    }

    public ScResource getCachedModel(Class<? extends ScResource> modelClass, long id) {
        if (Track.class.equals(modelClass)) {
            return mTrackCache.get(id);
        } else if (User.class.equals(modelClass)) {
            return mUserCache.get(id);
        }
        return null;
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
        }
        return model;
    }

    public User cache(User model) {
        if (mUserCache.containsKey(model.id)) {
            return mUserCache.get(model.id).updateFrom(model);
        }
        return model;
    }

    public Track put(Track t) {
        return mTrackCache.putWithLocalFields(t);
    }

}
