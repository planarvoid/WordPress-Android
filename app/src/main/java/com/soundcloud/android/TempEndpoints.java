package com.soundcloud.android;

import com.soundcloud.api.Endpoints;

public interface TempEndpoints {
    public interface e1 {
        String MY_STREAM = "/e1/me/stream";
        String MY_EXCLUSIVE_STREAM = MY_STREAM;//"/me/activities/tracks/exclusive";
        String MY_ACTIVITIES = "/e1/me/activities/";
    }
    public interface i1 {
        String ME_FACEBOOK_TOKEN = "/i1/me/facebook_token";
    }
}
