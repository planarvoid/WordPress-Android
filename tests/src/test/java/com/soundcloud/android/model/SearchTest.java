package com.soundcloud.android.model;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;

@RunWith(DefaultTestRunner.class)
public class SearchTest {
    @Test
    public void shouldBuildCorrectApiRequest() {
        expect(Search.forSounds("foo").request().toUrl()).toEqual("/search/sounds?q=foo");
        expect(Search.forPlaylists("foo").request().toUrl()).toEqual("/search/sets?q=foo");
        expect(Search.forUsers("foo").request().toUrl()).toEqual("/search/people?q=foo");
        expect(Search.forAll("foo").request().toUrl()).toEqual("/search?q=foo");
    }
}
