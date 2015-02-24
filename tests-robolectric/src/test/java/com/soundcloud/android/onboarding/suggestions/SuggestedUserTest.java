package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.Expect;
import com.soundcloud.android.onboarding.suggestions.SuggestedUser;
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
        Expect.expect(unparceledUser.getUsername()).toEqual(user.getUsername());
        Expect.expect(unparceledUser.getCity()).toEqual(user.getCity());
        Expect.expect(unparceledUser.getCountry()).toEqual(user.getCountry());
        Expect.expect(unparceledUser.getToken()).toEqual(user.getToken());
    }

}
