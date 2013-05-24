package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Genre;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedUsersAdapterTest {

    private SuggestedUsersAdapter adapter;

    @Before
    public void setup() {
        adapter = new SuggestedUsersAdapter();

        adapter.addItem(facebookLikes());
        adapter.addItem(facebookFriends());
        adapter.addItem(music());
        adapter.addItem(audio());
    }

    @Test
    public void shouldDetermineProperItemViewTypeToDistinguishSectionHeadersFromListElements() {
        adapter.notifyDataSetChanged();
        // -- FACEBOOK
        expect(adapter.getItemViewType(0)).toBe(SuggestedUsersAdapter.ItemViewType.HEADER.ordinal());
        expect(adapter.getItemViewType(1)).toBe(SuggestedUsersAdapter.ItemViewType.GENRE_BUCKET.ordinal());
        expect(adapter.getItemViewType(2)).toBe(SuggestedUsersAdapter.ItemViewType.GENRE_BUCKET.ordinal());
        // -- MUSIC
        expect(adapter.getItemViewType(3)).toBe(SuggestedUsersAdapter.ItemViewType.HEADER.ordinal());
        expect(adapter.getItemViewType(4)).toBe(SuggestedUsersAdapter.ItemViewType.GENRE_BUCKET.ordinal());
        // -- AUDIO
        expect(adapter.getItemViewType(5)).toBe(SuggestedUsersAdapter.ItemViewType.HEADER.ordinal());
        expect(adapter.getItemViewType(6)).toBe(SuggestedUsersAdapter.ItemViewType.GENRE_BUCKET.ordinal());
    }

    private GenreBucket audio() {
        Genre audioGenre = new Genre();
        audioGenre.setGrouping(Genre.Grouping.AUDIO);
        return new GenreBucket(audioGenre);
    }

    private GenreBucket music() {
        Genre musicGenre = new Genre();
        musicGenre.setGrouping(Genre.Grouping.MUSIC);
        return new GenreBucket(musicGenre);
    }

    private GenreBucket facebookFriends() {
        Genre fbFriendsGenre = new Genre();
        fbFriendsGenre.setGrouping(Genre.Grouping.FACEBOOK_FRIENDS);
        return new GenreBucket(fbFriendsGenre);
    }

    private GenreBucket facebookLikes() {
        Genre fbLikesGenre = new Genre();
        fbLikesGenre.setGrouping(Genre.Grouping.FACEBOOK_LIKES);
        return new GenreBucket(fbLikesGenre);
    }
}
