package com.soundcloud.android.tracking;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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

    @Test
    public void shouldExpandLevel2InArgs() throws Exception {
        User user = new User();
        user.permalink = "a_user";

        expect(Click.Follow.expandClick(Level2.Search, user)).toEqual("Follow::Search::a_user");
        expect(Click.Unfollow.expandClick(Level2.Search, user)).toEqual("Unfollow::Search::a_user");
    }

    @Test
    public void shouldExpandMultipleArgs() throws  Exception {
        expect(Click.Record_Share_Post.expandClick("some_tip", "trimmed", "fading"))
                .toEqual("record_share::post::some_tip::trimmed::fading");
    }
}
