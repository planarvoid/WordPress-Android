package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class SearchTest {
    @Test
    public void shouldBuildCorrectApiRequest() {
        expect(Search.forTracks("foo").request().toUrl()).toEqual("/search/sounds?q=foo");
        expect(Search.forPlaylists("foo").request().toUrl()).toEqual("/search/sets?q=foo");
        expect(Search.forUsers("foo").request().toUrl()).toEqual("/search/people?q=foo");
        expect(Search.forAll("foo").request().toUrl()).toEqual("/search?q=foo");
    }

    @Test
    public void shouldReturnCorrectScreen() {
        expect(Search.forTracks("foo").getScreen()).toEqual(Screen.SEARCH_TRACKS);
        expect(Search.forPlaylists("foo").getScreen()).toEqual(Screen.SEARCH_PLAYLISTS);
        expect(Search.forUsers("foo").getScreen()).toEqual(Screen.SEARCH_USERS);
        expect(Search.forAll("foo").getScreen()).toEqual(Screen.SEARCH_EVERYTHING);
    }
}
