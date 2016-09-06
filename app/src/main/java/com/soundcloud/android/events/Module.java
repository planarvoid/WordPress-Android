package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.profile.UserSoundsTypes;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

@AutoValue
public abstract class Module {
    public static final String STREAM = "stream";
    public static final String USER_SPOTLIGHT = "users-spotlight";
    public static final String USER_ALBUMS = "users-albums";
    public static final String USER_PLAYLISTS = "users-playlists";
    public static final String USER_REPOSTS = "users-reposts";
    public static final String USER_LIKES = "users-likes";
    public static final String USER_FOLLOWING = "users-followings";
    public static final String USER_FOLLOWERS = "users-followers";

    public static Module create(String name, String resource) {
        return new AutoValue_Module(name, resource);
    }

    public static Module create(String name) {
        return new AutoValue_Module(name, Strings.EMPTY);
    }

    public abstract String getName();

    public abstract String getResource();

    public static Optional<Module> getModuleFromUserSoundsType(int collectionType, String resource) {
        String moduleName = getModuleName(collectionType);
        if (moduleName != null) {
            return Optional.of(Module.create(moduleName, resource));
        } else {
            return Optional.absent();
        }
    }

    @Nullable
    private static String getModuleName(int collectionType) {
        switch (collectionType) {
            case UserSoundsTypes.ALBUMS:
                return Module.USER_ALBUMS;
            case UserSoundsTypes.PLAYLISTS:
                return Module.USER_PLAYLISTS;
            case UserSoundsTypes.LIKES:
                return Module.USER_LIKES;
            case UserSoundsTypes.REPOSTS:
                return Module.USER_REPOSTS;
            case UserSoundsTypes.SPOTLIGHT:
                return Module.USER_SPOTLIGHT;
            default:
                return null;
        }
    }
}
