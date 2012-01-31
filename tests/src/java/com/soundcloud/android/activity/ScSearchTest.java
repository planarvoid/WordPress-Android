package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Search;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ScSearchTest {
    ScSearch search;

    @Before
    public void before() {
        search = new ScSearch();
        // followings
        TestHelper.addResponseRule("/me/followings/ids", 404);
        search.onCreate(null);

        Robolectric.application.onCreate();
    }

    @Test
    public void shouldSearchTracks() throws Exception {
        fakeApiReplies("track_search.json");
        expect(search.perform(Search.forSounds("Testing"))).toBeTrue();
        search.mTrackAdpWrapper.executeAppendTask();

        expect(search.mTrackAdpWrapper.getData().size()).toEqual(3);
        expect(Content.SEARCHES).toHaveCount(1);
    }

    @Test
    public void shouldSearchUsers() throws Exception {
        fakeApiReplies("user_search.json");
        expect(search.perform(Search.forUsers("Testing"))).toBeTrue();
        search.mUserAdpWrapper.executeAppendTask();

        expect(search.mUserAdpWrapper.getData().size()).toEqual(3);
        expect(Content.SEARCHES).toHaveCount(1);
    }

    @Test
    public void shouldNotSearchOnEmptyString() throws Exception {
        expect(search.perform(Search.forSounds(""))).toBeFalse();
        expect(Content.SEARCHES).toBeEmpty();
    }

    @Test
    public void shouldNotSearchOnUnknownSearchType() throws Exception {
        expect(search.perform(new Search("Testing", 666))).toBeFalse();
        expect(Content.SEARCHES).toBeEmpty();
    }

    private void fakeApiReplies(String... resources) throws IOException {
        TestHelper.addCannedResponses(getClass(), resources);
    }
}
