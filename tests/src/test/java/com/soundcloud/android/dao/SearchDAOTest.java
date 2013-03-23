package com.soundcloud.android.dao;

import android.net.Uri;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.provider.Content;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

import java.util.List;

import static com.soundcloud.android.Expect.expect;

public class SearchDAOTest extends AbstractDAOTest<SearchDAO> {

    public SearchDAOTest() {
        super(new SearchDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void shouldInsertWithResolver() throws Exception {
        Search s = Search.forSounds("blaz");
        Uri u = SearchDAO.insert(Robolectric.application.getContentResolver(), s);
        expect(u).not.toBeNull();
        expect(u.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/searches/1");

        expect(Content.SEARCHES_ITEM).toHaveCount(1);

        List<Search> searches =  SearchDAO.getHistory(Robolectric.application.getContentResolver());
        expect(searches.size()).toEqual(1);
        Search item = searches.get(0);
        expect(item.search_type).toEqual(Search.SOUNDS);
        expect(item.query).toEqual("blaz");
        expect(item.created_at).not.toEqual(0L);
    }


    @Test
    public void shouldNotAddDuplicateSearches() throws Exception {
        for (int i=0; i<5; i++) {
            SearchDAO.insert(resolver, Search.forSounds("blaz"));
        }
        SearchDAO.insert(resolver, Search.forSounds("blaz-different"));
        expect(Content.SEARCHES_ITEM).toHaveCount(2);
    }

    @Test
    public void shouldClearHistory() throws Exception {
        for (int i=0; i<5; i++) {
            SearchDAO.insert(resolver,Search.forSounds("playable"+i));
        }
        expect(Content.SEARCHES_ITEM).toHaveCount(5);
        SearchDAO.clearState(resolver, USER_ID);
        expect(Content.SEARCHES_ITEM).toBeEmpty();
    }
}
