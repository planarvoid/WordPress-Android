package com.soundcloud.android.testsupport;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Shadow for {@link android.media.MediaRouter}.
 */
@Implements(android.support.v7.media.MediaRouter.class)
public class ShadowSupportMediaRouter {

    @Implementation
    public static android.support.v7.media.MediaRouter getInstance(@NonNull Context context) {
        return null;
    }

    @Implementation
    public int getRouteCount() {
        return 0;
    }
}
