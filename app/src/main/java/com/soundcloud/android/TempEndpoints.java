package com.soundcloud.android;

import com.soundcloud.api.Endpoints;

@Deprecated
public interface TempEndpoints {
    public interface e1 {
        String MY_STREAM = "/e1/me/stream";
        String MY_EXCLUSIVE_STREAM = MY_STREAM;//"/me/activities/tracks/exclusive";
        String MY_ACTIVITIES = "/e1/me/activities";
        String MY_REPOSTS = "/e1/me/track_reposts";
        String MY_REPOST = "/e1/me/track_reposts/%d";
        String USER_REPOSTS = "/e1/users/%/reposts";
        String TRACK_REPOSTERS = "/e1/tracks/%d/reposters";
    }
    public interface i1 {
        String ME_FACEBOOK_TOKEN = "/i1/me/facebook_token";
    }
}
