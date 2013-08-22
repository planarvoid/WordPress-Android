package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class MiniUserTest {

    @Test
    public void shouldBeParcelable() throws Exception {
        MiniUser miniUser1 = new MiniUser("soundcloud:users:123");
        miniUser1.setUsername("Slawomir");

        Parcel parcel = Parcel.obtain();
        miniUser1.writeToParcel(parcel, 0);

        final MiniUser miniUser2 = new MiniUser(parcel);
        expect(miniUser1.getId()).toEqual(miniUser2.getId());
        expect(miniUser1.getUrn()).toEqual(miniUser2.getUrn());
        expect(miniUser1.getUsername()).toEqual(miniUser2.getUsername());

    }
}
