package com.soundcloud.android.api.legacy.model.activities;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikeActivityTest {

    @Test
    public void shouldConvertToPropertySet() throws CreateModelException {
        TrackLikeActivity activity = new TrackLikeActivity();
        activity.track = ModelFixtures.create(PublicApiTrack.class);
        activity.user = ModelFixtures.create(PublicApiUser.class);
        activity.createdAt = new Date();

        final PropertySet propertySet = activity.toPropertySet();

        expect(propertySet.get(ActivityProperty.TYPE)).toEqual(ActivityProperty.TYPE_LIKE);
        expect(propertySet.get(ActivityProperty.USER_NAME)).toEqual(activity.getUser().getUsername());
        expect(propertySet.get(ActivityProperty.DATE)).toEqual(activity.getCreatedAt());
        expect(propertySet.get(ActivityProperty.SOUND_TITLE)).toEqual(activity.getPlayable().getTitle());
        expect(propertySet.get(ActivityProperty.USER_URN)).toEqual(activity.getUser().getUrn());
    }
}