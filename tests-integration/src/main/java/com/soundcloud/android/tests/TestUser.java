package com.soundcloud.android.tests;

import com.soundcloud.android.model.User;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.content.UserAssociationSyncer;
import rx.concurrency.Schedulers;

import android.content.Context;

import java.io.IOException;

public class TestUser {
    private final String username, email, password;

    public TestUser(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password= password;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }


    public static final TestUser scAccount      = new TestUser("Steven Testowy",        "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser scTestAccount  = new TestUser("android-testing",       "",                             "android-testing");
    public static final TestUser noGPlusAccount = new TestUser("Steven Testowy",        "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser GPlusAccount   = new TestUser("scandroidtest",         "sccloudandroid@gmail.com",     "s0undcl0ud");
    public static final TestUser Facebook       = new TestUser("Mike Smiechowy",        "ssmiechowy@gmail.com",         "passwordyeah");
    public static final TestUser testUser       = new TestUser("Slawomir Smiechowy 2",  "test26-82@wp.pl",              "password");

    public static void unfollowAll(Context context) {
        try {
            new UserAssociationSyncer(context).syncContent(Content.ME_FOLLOWINGS.uri, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final FollowingOperations followingOperations = new FollowingOperations(Schedulers.immediate());
        for (long userId : followingOperations.getFollowedUserIds()){
            followingOperations.removeFollowing(new User(userId));
        }

        try {
            new UserAssociationSyncer(context).syncContent(Content.ME_FOLLOWINGS.uri, ApiSyncService.ACTION_PUSH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
