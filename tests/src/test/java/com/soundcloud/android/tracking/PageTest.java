package com.soundcloud.android.tracking;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class PageTest {

    @Test
    public void shouldExpandPageNameWithTrack() throws Exception {
        Track t = new Track();
        t.permalink = "a_track";
        t.user = new User();
        t.user.permalink = "a_user";

        expect(Page.Sounds_add_comment.expandPage(t)).toEqual("a_user::a_track::add_comment");
    }

    @Test
    public void shouldExpandPageNameWithUser() throws Exception {
        User user = new User();
        user.permalink = "a_user";

        expect(Page.Users_dedicated_rec.expandPage(user)).toEqual("a_user::dedicated_rec");
    }

    @Test
    public void shouldNotExpandWithoutArgs() throws Exception {
        expect(Page.Users_dedicated_rec.expandPage()).toEqual(Page.Users_dedicated_rec.name);
    }

    @Test
    public void shouldExpandPageWithString() throws Exception {
        expect(Page.Search_results__people__keyword.expandPage("foo")).toEqual("results::people::foo");
    }

    @Test
    public void shouldExpandPageNameWithTrackAndNulls() throws Exception {
        Track t = new Track();
        t.permalink = "a_track";
        t.user = new User();
        expect(Page.Sounds_add_comment.expandPage(t)).toEqual("user_permalink::a_track::add_comment");
    }

    @Test
    public void shouldExpandPageNameWithTrackAndNullUser() throws Exception {
        Track t = new Track();
        t.permalink = "a_track";
        expect(Page.Sounds_add_comment.expandPage(t)).toEqual("user_permalink::a_track::add_comment");
    }
}
