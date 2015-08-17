package com.soundcloud.android.sync;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.jetbrains.annotations.Nullable;

public class ActivityFixtures {

    public static Activity forComment() {
        return createActivity(Activity.Type.COMMENT);
    }

    public static Activity forLike() {
        return createActivity(Activity.Type.TRACK_LIKE);
    }

    public static Activity forRepost() {
        return createActivity(Activity.Type.TRACK_REPOST);
    }

    public static Activity forNewFollower() {
        return createActivity(Activity.Type.AFFILIATION);
    }

    private static Activity createActivity(Activity.Type trackLike) {
        final PublicApiUser apiUser = ModelFixtures.create(PublicApiUser.class);
        final PublicApiTrack apiTrack = ModelFixtures.create(PublicApiTrack.class);
        return new ActivityStub(trackLike, apiUser, apiTrack);
    }

    private static class ActivityStub extends Activity {

        private final Type type;
        private final PublicApiUser user;
        private final Playable playable;

        ActivityStub(Type type, PublicApiUser user, Playable playable) {
            this.type = type;
            this.user = user;
            this.playable = playable;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public PublicApiUser getUser() {
            return user;
        }

        @Override
        public Playable getPlayable() {
            return playable;
        }

        @Override
        public void cacheDependencies() {
            // no-op
        }

        @Nullable
        @Override
        public Refreshable getRefreshableResource() {
            // no-op
            return null;
        }

        @Override
        public boolean equals(Object that) {
            return that == this;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
