package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUserTest {

    private SuggestedUser user = new SuggestedUser("soundcloud:users:1");

    @Test
    public void shouldBeParcelable() {
        user.setUsername("JonSchmidt");
        user.setCity("Berlin");
        user.setCountry("Germany");
        user.setToken("JUs51mg8aMqsisKlsCrzuuZVILNLFJKMVUYK/OEXAWbazqNWZiPZGhWprfsk\\nnKK6M28iNFBzhNJot3o5AL/5sQ==\\n");

        Parcel parcel = Parcel.obtain();
        user.writeToParcel(parcel, 0);

        SuggestedUser unparceledUser = new SuggestedUser(parcel);
        expect(unparceledUser.getUsername()).toEqual(user.getUsername());
        expect(unparceledUser.getCity()).toEqual(user.getCity());
        expect(unparceledUser.getCountry()).toEqual(user.getCountry());
        expect(unparceledUser.getToken()).toEqual(user.getToken());
    }

}
