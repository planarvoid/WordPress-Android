package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class UserSummaryTest {

    @Test
    public void shouldBeParcelable() throws Exception {
        UserSummary userSummary1 = new UserSummary("soundcloud:users:123");
        userSummary1.setUsername("Slawomir");
        userSummary1.setAvatarUrl("avatar/url");

        Parcel parcel = Parcel.obtain();
        userSummary1.writeToParcel(parcel, 0);

        final UserSummary userSummary2 = new UserSummary(parcel);
        expect(userSummary1.getId()).toEqual(userSummary2.getId());
        expect(userSummary1.getUrn()).toEqual(userSummary2.getUrn());
        expect(userSummary1.getUsername()).toEqual(userSummary2.getUsername());
        expect(userSummary1.getAvatarUrl()).toEqual(userSummary2.getAvatarUrl());

    }
}
