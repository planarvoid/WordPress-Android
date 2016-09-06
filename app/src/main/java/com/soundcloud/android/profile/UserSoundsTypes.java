package com.soundcloud.android.profile;

import com.soundcloud.android.events.Module;

public final class UserSoundsTypes {
    public static final int SPOTLIGHT = 0;
    public static final int TRACKS = 1;
    public static final int ALBUMS = 2;
    public static final int PLAYLISTS = 3;
    public static final int REPOSTS = 4;
    public static final int LIKES = 5;

    public static Module fromModule(int collectionType, int position) {
        return Module.create(getModuleName(collectionType), position);
    }

    private static String getModuleName(int collectionType) {
        switch (collectionType) {
            case UserSoundsTypes.TRACKS:
                return Module.USER_TRACKS;
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
                throw new IllegalArgumentException("Unknown UserSoundType: " + collectionType);
        }
    }
}
