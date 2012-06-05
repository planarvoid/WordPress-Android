package com.soundcloud.android.model;

import static com.soundcloud.android.AndroidCloudAPI.CloudDateFormat.fromString;
import static com.soundcloud.android.AndroidCloudAPI.CloudDateFormat.toTime;
import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@RunWith(DefaultTestRunner.class)
public class ActivitiesTest {

    @Test
    public void testIsEmpty() throws Exception {
        Activities activities = new Activities();
        expect(activities.isEmpty()).toBeTrue();
        expect(activities.size()).toEqual(0);
    }

    @Test
    public void testFull() throws Exception {
        Activities activities = new Activities(new Activity(), new Activity());
        expect(activities.isEmpty()).toEqual(false);
        expect(activities.size()).toEqual(2);
    }

    @Test
    public void testGetUniqueUsers() throws Exception {
        Activities activities = new Activities();
        expect(activities.getUniqueUsers().size()).toEqual(0);
        Activity e1 = new Activity() { public User getUser() { return new User() { { id = 1; } }; } };
        Activity e2 = new Activity() { public User getUser() { return new User() { { id = 1; } }; } };
        Activity e3 = new Activity() { public User getUser() { return new User() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        expect(activities.getUniqueUsers().size()).toEqual(2);
    }

    @Test
    public void testGetUniqueTracks() throws Exception {
        Activities activities = new Activities();
        expect(activities.getUniqueTracks().size()).toEqual(0);
        Activity e1 = new Activity() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Activity e2 = new Activity() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Activity e3 = new Activity() { public Track getTrack() { return new Track() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        expect(activities.getUniqueTracks().size()).toEqual(2);
    }

    @Test
    public void testFromJSON() throws Exception {
        Activities a = getActivities();
        expect(a.size()).toEqual(41);
        expect(a.getUniqueTracks().size()).toEqual(19);
        expect(a.getUniqueUsers().size()).toEqual(29);
    }

    @Test
    public void testFavoritings() throws Exception {
        Activities favoritings = getActivities().favoritings();
        expect(favoritings.size()).toEqual(26);
    }

    @Test
    public void testTracks() throws Exception {
        Activities tracks = getActivities().tracks();
        expect(tracks.size()).toEqual(0);
    }

    @Test
    public void testSharings() throws Exception {
        Activities sharings = getActivities().sharings();
        expect(sharings.size()).toEqual(0);
    }

    @Test
    public void testComments() throws Exception {
        Activities comments = getActivities().comments();
        expect(comments.size()).toEqual(15);
    }

    @Test
    public void testGroupedByTrack() throws Exception {
        Map<Track,Activities> grouped = getActivities().groupedByTrack();
        expect(grouped.size()).toEqual(19);
        for (Map.Entry<Track,Activities> entry : grouped.entrySet()) {
            expect(entry.getKey()).not.toBeNull();
            expect(entry.getValue().isEmpty()).toEqual(false);
        }
    }

    @Test
    public void testOriginIsSetOnAllActivities() throws Exception {
        for (Activity e : getActivities()) {
            expect(e.origin).not.toBeNull();
        }
    }

    @Test
    public void testGetCursor() throws Exception {
        Activities activities = new Activities();
        expect(activities.getCursor()).toBeNull();
        activities.next_href = "http://foo.com?cursor=dada";
        expect(activities.getCursor()).toEqual("dada");
    }

    private Activities getActivities() throws IOException {
        return Activities.fromJSON(getClass().getResourceAsStream("activities_all.json"));
    }

    @Test
    public void testSubTypes() throws Exception {
        for (Activity a : getActivities()) {
            expect(a.origin.getClass().equals(a.type.typeClass)).toBeTrue();
        }
    }

    @Test
    public void testMerge() throws Exception {
        Activities a1 = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        Activities a2 = Activities.fromJSON(getClass().getResourceAsStream("activities_2.json"));
        Activities all = Activities.fromJSON(getClass().getResourceAsStream("activities_all.json"));

        Activities merged = a2.merge(a1);
        expect(merged.size()).toEqual(all.size());

        expect(merged.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=new_href");
        expect(merged.next_href).toEqual("https://api.soundcloud.com/me/activities/tracks?cursor=e46666c4-a7e6-11e0-8c30-73a2e4b61738");
        expect(merged.get(0).created_at.after(merged.get(merged.size()-1).created_at)).toBeTrue();
    }

    @Test
    public void testMergeWithEmpty() throws Exception {
        Activities a1 = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        expect(a1.merge(Activities.EMPTY)).toBe(a1);
    }

    @Test
    public void testFilter() throws Exception {
        Activities a2 = Activities.fromJSON(getClass().getResourceAsStream("activities_2.json"));
        Date start = fromString("2011/07/29 15:36:44 +0000");

        Activities filtered = a2.filter(start);
        expect(filtered.size()).toEqual(1);
        expect(filtered.get(0).created_at.after(start)).toBeTrue();
    }

    @Test
    public void testGetNextRequest() throws Exception {
        Activities a1 = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        expect(a1.hasMore()).toBeTrue();
        expect(a1.getNextRequest().toUrl()).toEqual("/me/activities/tracks?cursor=e46666c4-a7e6-11e0-8c30-73a2e4b61738");
    }

    @Test
    public void shouldFetchFromApi() throws Exception {
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class,
                "incoming_1.json",
                "incoming_2.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application, Content.ME_SOUND_STREAM.request(), 100);
        expect(a.size()).toEqual(100);
        expect(a.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
        expect(a.hasMore()).toBeFalse();
    }

    @Test
    public void shouldFetchOnlyUpToMaxItems() throws Exception {
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class,
                "incoming_1.json");

        Activities a = Activities.fetchRecent(DefaultTestRunner.application, Content.ME_SOUND_STREAM.request(), 50);
        expect(a.size()).toEqual(50);
        expect(a.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
        expect(a.hasMore()).toBeTrue();
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        Activities a = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        ContentValues[] cv = a.buildContentValues(-1);
        expect(cv.length).toEqual(a.size());
    }

    @Test
    public void shouldInsertActivitiesIntoDb() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));
        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        expect(Content.ME_SOUND_STREAM).toHaveCount(50);
    }

    @Test
    public void shouldGetActivitiesFromDB() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));
        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        expect(Content.ME_SOUND_STREAM).toHaveCount(50);

        expect(
            Activities.getSince(Content.ME_SOUND_STREAM,
                    Robolectric.application.getContentResolver(), -1).size()
        ).toEqual(50);
    }

    @Test
    public void shouldGetActivitiesFromDBWithTimeFiltering() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));
        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        expect(Content.ME_SOUND_STREAM).toHaveCount(50);

        expect(
                Activities.getSince(Content.ME_SOUND_STREAM,
                        Robolectric.application.getContentResolver(),
                        toTime("2011/07/12 09:13:36 +0000")).size()
        ).toEqual(2);
    }

    @Test
    public void shouldGetLastActivity() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));
        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        expect(Content.ME_SOUND_STREAM).toHaveCount(50);

        expect(
                Activities.getLastActivity(Content.ME_SOUND_STREAM,
                        Robolectric.application.getContentResolver()).created_at.getTime()
        ).toEqual(toTime("2011/07/06 15:47:50 +0000"));
    }

    @Test
    public void shouldClearAllActivities() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));

        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        expect(Content.ME_SOUND_STREAM).toHaveCount(50);

        LocalCollection.insertLocalCollection(Content.ME_SOUND_STREAM.uri,
                0, System.currentTimeMillis(), System.currentTimeMillis(), a.size(),a.future_href,
                Robolectric.application.getContentResolver());

        Activities.clear(null, Robolectric.application.getContentResolver());

        expect(Content.ME_SOUND_STREAM).toHaveCount(0);
        expect(Content.COLLECTIONS).toHaveCount(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfContentPassedToClearIsUnrelated() throws Exception {
        Activities.clear(Content.ME, Robolectric.application.getContentResolver());
    }

    @Test
    public void shouldPersistAllActivityTypes() throws Exception {
        Activities a = Activities.fromJSON(
                getClass().getResourceAsStream("one_of_each_activity_type.json"));

        // need to insert track owner for joins to work
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, 133201L);
        cv.put(DBHelper.Users.USERNAME, "Foo Bar");

        ContentResolver resolver = Robolectric.application.getContentResolver();
        resolver.insert(Content.USERS.uri, cv);

        a.insert(Content.ME_ACTIVITIES, resolver);

        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(4);

        Activities activities = Activities.getSince(Content.ME_ALL_ACTIVITIES, resolver, -1);
        expect(activities.size()).toEqual(4);

        Activity track = activities.get(2);
        expect(track.type).toEqual(Activity.Type.TRACK);
        expect(track.getDateString()).toEqual("2011/07/12 09:27:19 +0000");
        expect(track.tags).toEqual("affiliated, exclusive");
        expect(track.getTrack().id).toEqual(18876167L);
        expect(track.getTrack().permalink).toEqual("grand-piano-keys");
        expect(track.getTrack().title).toEqual("Grand Piano Keys");
        expect(track.getTrack().artwork_url).toEqual("http://i1.sndcdn.com/artworks-000009195725-njfi16-large.jpg?a1786a9");
        expect(track.getTrack().sharing_note).not.toBeNull();
        expect(track.getTrack().sharing_note.text).toEqual("Bla Bla Bla");

        expect(track.getTrack().user.id).toEqual(3207L);
        expect(track.getTrack().user.permalink).toEqual("jwagener");

        Activity sharing = activities.get(3);
        expect(sharing.type).toEqual(Activity.Type.TRACK_SHARING);
        expect(sharing.getDateString()).toEqual("2011/07/11 20:42:34 +0000");
        expect(sharing.getTrack().id).toEqual(18676478L);
        expect(sharing.getTrack().permalink).toEqual("live-in-vegas");
        expect(sharing.getTrack().title).toEqual("Live in Vegas");
        expect(sharing.getTrack().sharing_note).not.toBeNull();
        expect(sharing.getTrack().sharing_note.text).toEqual("Enjoy, share, and dont be shy leave me your thoughts!");

        Activity comment = activities.get(1);
        expect(comment.type).toEqual(Activity.Type.COMMENT);
        expect(comment.getDateString()).toEqual("2011/07/29 15:26:44 +0000");
        expect(comment.getComment()).not.toBeNull();
        expect(comment.getComment().id).toEqual(22140210L);

        expect(comment.getTrack().id).toEqual(20023414L);
        expect(comment.getTrack().permalink).toEqual("sounds-from-dalston-kingsland");
        expect(comment.getTrack().title).toEqual("Sounds from Dalston Kingsland Railway Station (DLK)");
        expect(comment.getTrack().user_id).toEqual(133201L);
        expect(comment.getTrack().user.username).toEqual("Foo Bar");

        Activity favoriting = activities.get(0);

        expect(favoriting.type).toEqual(Activity.Type.FAVORITING);
        expect(favoriting.getDateString()).toEqual("2011/07/29 22:30:59 +0000");
        expect(favoriting.getFavoriting()).not.toBeNull();

        expect(favoriting.getFavoriting().track).toBe(favoriting.getTrack());

        expect(favoriting.getTrack().id).toEqual(13090155L);
        expect(favoriting.getTrack().permalink).toEqual("p-watzlawick-anleitung-zum");
        expect(favoriting.getTrack().title).toEqual("P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
        expect(favoriting.getTrack().user_id).toEqual(133201L);
        expect(favoriting.getTrack().user.username).toEqual("Foo Bar");
    }

    @Test
    public void shouldGetArtworkUrls() throws Exception {
        Activities a = Activities.fromJSON(
                getClass().getResourceAsStream("one_of_each_activity_type.json"));

        Set<String> urls = a.artworkUrls();
        expect(urls.size()).toEqual(3);
        expect(urls).toContain(
            "http://i1.sndcdn.com/artworks-000009086878-mwsj4x-large.jpg?a1786a9",
            "http://i1.sndcdn.com/artworks-000009823303-xte9r2-large.jpg?8935bc4",
            "http://i1.sndcdn.com/artworks-000009195725-njfi16-large.jpg?a1786a9"
        );
    }

    @Test
    public void shouldReturnCorrectIcon() throws Exception {
        String artwork_1 = "artwork1.jpg";
        String artwork_2 = "artwork2.jpg";
        String avatar_1 = "avatar1.jpg";
        String avatar_2 = "avatar2.jpg";

        Activities a = new Activities();
        a.add(makeActivity(makeTrack(null,null)));
        a.add(makeActivity(makeTrack(null,null)));
        expect(a.getFirstAvailableArtwork()).toBeNull();
        expect(a.getFirstAvailableAvatar()).toBeNull();

        a = new Activities();
        a.add(makeActivity(makeTrack(null,artwork_1)));
        a.add(makeActivity(makeTrack(null,artwork_2)));
        expect(a.getFirstAvailableArtwork()).toEqual(artwork_1);
        expect(a.getFirstAvailableAvatar()).toBeNull();

        a = new Activities();
        a.add(makeActivity(makeTrack(makeUser(avatar_1),null)));
        a.add(makeActivity(makeTrack(null,artwork_2)));
        expect(a.getFirstAvailableArtwork()).toEqual(artwork_2);
        expect(a.getFirstAvailableAvatar()).toEqual(avatar_1);

        a = new Activities();
        a.add(makeActivity(makeTrack(null, null)));
        a.add(makeActivity(makeTrack(makeUser(avatar_2),null)));
        expect(a.getFirstAvailableArtwork()).toEqual(avatar_2);
        expect(a.getFirstAvailableAvatar()).toEqual(avatar_2);
    }

    @Test
    public void shouldNotCreateNewUserObjectsIfObjectIdIsTheSame() throws Exception {

        Activities a = Activities.fromJSON(
                getClass().getResourceAsStream("two_activities_by_same_user.json"));

        // fronx favorites + comments
        User u1 = a.get(0).getUser();
        User u2 = a.get(1).getUser();

        expect(u1).toBe(u2);
    }


    private Activity makeActivity(Track t){
        Activity a = new Activity();
        a.origin = t;
        return a;
    }

    private Track makeTrack(@Nullable User u, @Nullable String artworkUrl){
        Track t = new Track();
        t.artwork_url = artworkUrl;
        t.user = u;
        return t;
    }

    private User makeUser(String avatarUrl){
        User u = new User();
        u.avatar_url = avatarUrl;
        return u;
    }

}
