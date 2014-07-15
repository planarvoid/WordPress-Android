package com.soundcloud.android.api.model;

import com.soundcloud.android.Expect;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class ApiUserTest {

    @Test
    public void shouldBeParcelable() throws Exception {
        ApiUser userSummary1 = new ApiUser("soundcloud:users:123");
        userSummary1.setUsername("Slawomir");
        userSummary1.setAvatarUrl("avatar/url");

        Parcel parcel = Parcel.obtain();
        userSummary1.writeToParcel(parcel, 0);

        final ApiUser userSummary2 = new ApiUser(parcel);
        Expect.expect(userSummary1.getId()).toEqual(userSummary2.getId());
        Expect.expect(userSummary1.getUrn()).toEqual(userSummary2.getUrn());
        Expect.expect(userSummary1.getUsername()).toEqual(userSummary2.getUsername());
        Expect.expect(userSummary1.getAvatarUrl()).toEqual(userSummary2.getAvatarUrl());

    }
}
