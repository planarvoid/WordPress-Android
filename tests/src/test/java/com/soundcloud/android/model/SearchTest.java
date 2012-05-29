package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class SearchTest {
    ContentResolver resolver;
    final static long USER_ID = 1L;

    @Before
    public void before() {
        resolver = DefaultTestRunner.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldInsertWithResolver() throws Exception {
        Search s = Search.forSounds("blaz");
        Uri u = s.insert(Robolectric.application.getContentResolver());
        expect(u).not.toBeNull();
        expect(u.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/searches/1");

        expect(Content.SEARCH).toHaveCount(1);

        List<Search> searches =  Search.getHistory(Robolectric.application.getContentResolver());
        expect(searches.size()).toEqual(1);
        Search item = searches.get(0);
        expect(item.search_type).toEqual(Search.SOUNDS);
        expect(item.query).toEqual("blaz");
        expect(item.created_at).not.toEqual(0L);
    }

    @Test
    public void shouldNotAddDuplicateSearches() throws Exception {
        for (int i=0; i<5; i++) {
            Search.forSounds("blaz").insert(resolver);
        }
        Search.forSounds("blaz-different").insert(resolver);
        expect(Content.SEARCH).toHaveCount(2);
    }

    @Test
    public void shouldClearHistory() throws Exception {
        for (int i=0; i<5; i++) {
            Search.forSounds("sound"+i).insert(resolver);
        }

        expect(Content.SEARCH).toHaveCount(5);
        Search.clearState(resolver, USER_ID);
        expect(Content.SEARCH).toBeEmpty();
    }
}
