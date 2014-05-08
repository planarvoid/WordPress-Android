package com.soundcloud.android.tests;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.HttpProperties;
import com.soundcloud.android.api.http.PublicApiWrapper;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.content.UserAssociationSyncer;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import rx.schedulers.Schedulers;

import android.app.Activity;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

public class TestUser {
    private final String username, email, password;

    public TestUser(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
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

    public boolean logIn(Context context) {
        ApiWrapper apiWrapper = createApiWrapper(context);
        try {
            Token token = apiWrapper.login(username, password, Token.SCOPE_NON_EXPIRING);
            User user = getLoggedInUser(apiWrapper);
            return SoundCloudApplication.fromContext(context).addUserAccountAndEnableSync(user, token, SignupVia.NONE);

        } catch (IOException e) {
            throw new AssertionError("error logging in: " + e.getMessage());
        }
    }

    private ApiWrapper createApiWrapper(Context context) {
        final HttpProperties properties = new HttpProperties(context.getResources());
        return new ApiWrapper(properties.getClientId(), properties.getClientSecret(), PublicApiWrapper.ANDROID_REDIRECT_URI, null);
    }

    private User getLoggedInUser(ApiWrapper apiWrapper) throws IOException {
        final InputStream content = apiWrapper.get(Request.to(Endpoints.MY_DETAILS)).getEntity().getContent();
        return PublicApiWrapper.buildObjectMapper().readValue(content, User.class);
    }

    public void unfollowAll(Activity activity) {
        final Context context =  activity.getApplicationContext();
        try {
            new UserAssociationSyncer(context).syncContent(Content.ME_FOLLOWINGS.uri, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final FollowingOperations followingOperations = new FollowingOperations(Schedulers.immediate());
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                for (long userId : followingOperations.getFollowedUserIds()){
                    followingOperations.removeFollowing(new User(userId));
                }

                try {
                    new UserAssociationSyncer(context).syncContent(Content.ME_FOLLOWINGS.uri, ApiSyncService.ACTION_PUSH);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    public static final TestUser defaultUser    = new TestUser("android-testing",       "",                             "android-testing");
    public static final TestUser scAccount      = new TestUser("Steven Testowy",        "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser scTestAccount  = new TestUser("android-testing",       "",                             "android-testing");
    public static final TestUser noGPlusAccount = new TestUser("Steven Testowy",        "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser GPlusAccount   = new TestUser("scandroidtest",         "sccloudandroid@gmail.com",     "s0undcl0ud");
    public static final TestUser Facebook       = new TestUser("Mike Smiechowy",        "ssmiechowy@gmail.com",         "passwordyeah2");
    public static final TestUser playlistUser   = new TestUser("mike-smiechowy",        "ssmiechowy@gmail.com",         "passwordyeah");
    public static final TestUser testUser       = new TestUser("Slawomir Smiechowy 2",  "test26-82@wp.pl",              "password");
    public static final TestUser emptyUser      = new TestUser("scEmpty",               "scemptyuser@gmail.com",        "s0undcl0ud");
    public static final TestUser followedUser   = new TestUser("android-followed",      "sctestfollowed@gmail.com",     "followed");


}
