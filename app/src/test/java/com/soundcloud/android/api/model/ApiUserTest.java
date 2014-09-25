package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class ApiUserTest {

    @Test
    public void shouldBeParcelable() {
        ApiUser userSummary1 = new ApiUser("soundcloud:users:123");
        userSummary1.setUsername("Slawomir");
        userSummary1.setAvatarUrl("avatar/url");

        Parcel parcel = Parcel.obtain();
        userSummary1.writeToParcel(parcel, 0);

        final ApiUser userSummary2 = new ApiUser(parcel);
        expect(userSummary1.getId()).toEqual(userSummary2.getId());
        expect(userSummary1.getUrn()).toEqual(userSummary2.getUrn());
        expect(userSummary1.getUsername()).toEqual(userSummary2.getUsername());
        expect(userSummary1.getAvatarUrl()).toEqual(userSummary2.getAvatarUrl());
    }

    @Test
    public void shouldTurnToPropertySet() {
        ApiUser user = ModelFixtures.create(ApiUser.class);

        PropertySet propertySet = user.toPropertySet();
        expect(propertySet.get(UserProperty.URN)).toEqual(user.getUrn());
        expect(propertySet.get(UserProperty.USERNAME)).toEqual(user.getUsername());
        expect(propertySet.get(UserProperty.COUNTRY)).toEqual(user.getCountry());
        expect(propertySet.get(UserProperty.FOLLOWERS_COUNT)).toEqual(user.getFollowersCount());
    }
}
