package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(DefaultTestRunner.class)
public class FavoritingTest {
    @Test
    public void shouldBeParcelable() throws Exception {
        Favoriting f1 = new Favoriting();
        f1.user = new User();
        f1.track = new Track();

        Parcel p = Parcel.obtain();
        f1.writeToParcel(p, 0);

        Favoriting f2 = Favoriting.CREATOR.createFromParcel(p);

        expect(f1.user).toEqual(f2.user);
        expect(f1.track).toEqual(f2.track);
    }
}
