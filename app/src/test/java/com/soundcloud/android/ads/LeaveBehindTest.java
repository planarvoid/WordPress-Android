package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class LeaveBehindTest {

    @Test
    public void shouldResolveToPropertySet() throws CreateModelException {
        LeaveBehind leaveBehind = ModelFixtures.create(LeaveBehind.class);

        final PropertySet propertySet = leaveBehind.toPropertySet();

        expect(propertySet.contains(LeaveBehindProperty.LEAVE_BEHIND_URN)).toBeTrue();
        expect(propertySet.contains(LeaveBehindProperty.IMAGE_URL)).toBeTrue();
        expect(propertySet.contains(LeaveBehindProperty.CLICK_THROUGH_URL)).toBeTrue();
        expect(propertySet.contains(LeaveBehindProperty.TRACKING_CLICK_URLS)).toBeTrue();
        expect(propertySet.contains(LeaveBehindProperty.TRACKING_IMPRESSION_URLS)).toBeTrue();
    }

}