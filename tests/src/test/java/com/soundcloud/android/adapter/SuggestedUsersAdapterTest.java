package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Genre;
import com.soundcloud.android.model.GenreBucket;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Map;

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
    public void shouldMapCorrectlyFromGenreGroupingsToListSections() {
        Genre.Grouping fbLikes = facebookLikes().getGenre().getGrouping();
        expect(SuggestedUsersAdapter.Section.fromGenreGrouping(fbLikes)).toEqual(SuggestedUsersAdapter.Section.FACEBOOK);

        Genre.Grouping fbFriends = facebookFriends().getGenre().getGrouping();
        expect(SuggestedUsersAdapter.Section.fromGenreGrouping(fbFriends)).toEqual(SuggestedUsersAdapter.Section.FACEBOOK);

        Genre.Grouping music = music().getGenre().getGrouping();
        expect(SuggestedUsersAdapter.Section.fromGenreGrouping(music)).toEqual(SuggestedUsersAdapter.Section.MUSIC);

        Genre.Grouping audio = audio().getGenre().getGrouping();
        expect(SuggestedUsersAdapter.Section.fromGenreGrouping(audio)).toEqual(SuggestedUsersAdapter.Section.AUDIO);
    }

    @Test
    public void shouldBuildListPositionsToSectionsMapWhileAddingNewItems() {
        Map<Integer, SuggestedUsersAdapter.Section> sectionMap = adapter.getListPositionsToSectionsMap();

        expect(sectionMap).not.toBeNull();
        expect(sectionMap.size()).toBe(3);
        expect(sectionMap.get(0)).toEqual(SuggestedUsersAdapter.Section.FACEBOOK); // 2 items under Facebook
        expect(sectionMap.get(2)).toEqual(SuggestedUsersAdapter.Section.MUSIC); // 1 item under Music
        expect(sectionMap.get(3)).toEqual(SuggestedUsersAdapter.Section.AUDIO); // 1 item under Audio
    }

    @Test
    public void shouldGetViewWithHeader() {
        final int positionWithHeader = 0;
        View itemLayout = adapter.getView(positionWithHeader, null, new FrameLayout(Robolectric.application));

        expect(itemLayout).not.toBeNull();
        View headerView = itemLayout.findViewById(R.id.suggested_users_list_header);
        expect(headerView).not.toBeNull();
        expect(headerView.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldGetViewWithoutHeader() {
        final int positionWithoutHeader = 1;
        View itemLayout = adapter.getView(positionWithoutHeader, null, new FrameLayout(Robolectric.application));

        expect(itemLayout).not.toBeNull();
        View headerView = itemLayout.findViewById(R.id.suggested_users_list_header);
        expect(headerView).not.toBeNull();
        expect(headerView.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldSetCorrectBucketTextForSingleUser() {
        GenreBucket bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex");
    }

    @Test
    public void shouldSetCorrectBucketTextForTwoUsers() {
        GenreBucket bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(buildUser("Skrillex"), buildUser("Forss")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex, Forss");
    }

    @Test
    public void shouldSetCorrectBucketTextForMultipleUsers() {
        GenreBucket bucket = adapter.getItem(0);
        bucket.setUsers(Lists.newArrayList(
                buildUser("Skrillex"), buildUser("Forss"), buildUser("Rick Astley")));

        View itemLayout = adapter.getView(0, null, new FrameLayout(Robolectric.application));

        TextView textView = (TextView) itemLayout.findViewById(android.R.id.text2);
        expect(textView.getText()).toEqual("Skrillex, Forss and 1 other");
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

    private User buildUser(String name) {
        User user = new User();
        user.username = name;
        return user;
    }
}
