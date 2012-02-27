package com.soundcloud.android.tracking;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class ClickTest {

    @Test
    public void shouldExpandArgs() throws Exception {
        Track t = new Track();
        t.permalink = "foo";
        t.user = new User();
        t.user.permalink = "user";

        expect(Click.Like.expandClick(t)).toEqual("Like::user::foo");
    }
}
