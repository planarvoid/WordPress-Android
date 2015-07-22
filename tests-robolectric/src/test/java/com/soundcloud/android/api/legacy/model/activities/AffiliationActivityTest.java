package com.soundcloud.android.api.legacy.model.activities;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class AffiliationActivityTest {

    @Test
    public void shouldConvertToPropertySet() throws CreateModelException {
        AffiliationActivity activity = new AffiliationActivity();
        activity.setUser(ModelFixtures.create(PublicApiUser.class));
        activity.createdAt = new Date();


        final PropertySet propertySet = activity.toPropertySet();

        expect(propertySet.get(ActivityProperty.TYPE)).toEqual(ActivityProperty.TYPE_FOLLOWER);
        expect(propertySet.get(ActivityProperty.USER_NAME)).toEqual(activity.getUser().getUsername());
        expect(propertySet.get(ActivityProperty.USER_URN)).toEqual(activity.getUser().getUrn());
        expect(propertySet.get(ActivityProperty.DATE)).toEqual(activity.getCreatedAt());
    }

}