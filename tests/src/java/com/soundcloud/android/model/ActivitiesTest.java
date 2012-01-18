package com.soundcloud.android.model;

import static com.soundcloud.android.AndroidCloudAPI.CloudDateFormat.*;
import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.assertContentUriCount;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import org.codehaus.jackson.JsonNode;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@RunWith(DefaultTestRunner.class)
public class ActivitiesTest {

    @Test
    public void testIsEmpty() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.isEmpty(), is(true));
        assertThat(activities.size(), is(0));
    }

    @Test
    public void testFull() throws Exception {
        Activities activities = new Activities(new Activity(), new Activity());
        assertThat(activities.isEmpty(), is(false));
        assertThat(activities.size(), is(2));
    }

    @Test
    public void testGetUniqueUsers() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getUniqueUsers().size(), is(0));
        Activity e1 = new Activity() { public User getUser() { return new User() { { id = 1; } }; } };
        Activity e2 = new Activity() { public User getUser() { return new User() { { id = 1; } }; } };
        Activity e3 = new Activity() { public User getUser() { return new User() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        assertThat(activities.getUniqueUsers().size(), is(2));
    }

    @Test
    public void testGetUniqueTracks() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getUniqueTracks().size(), is(0));
        Activity e1 = new Activity() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Activity e2 = new Activity() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Activity e3 = new Activity() { public Track getTrack() { return new Track() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        assertThat(activities.getUniqueTracks().size(), is(2));
    }

    @Test
    public void testFromJSON() throws Exception {
        Activities a = getActivities();
        assertThat(a.size(), is(41));
        assertThat(a.getUniqueTracks().size(), is(19));
        assertThat(a.getUniqueUsers().size(), is(29));
    }

    @Test
    public void testFavoritings() throws Exception {
        Activities favoritings = getActivities().favoritings();
        assertThat(favoritings.size(), is(26));
    }

    @Test
    public void testTracks() throws Exception {
        Activities tracks = getActivities().tracks();
        assertThat(tracks.size(), is(0));
    }

    @Test
    public void testSharings() throws Exception {
        Activities sharings = getActivities().sharings();
        assertThat(sharings.size(), is(0));
    }

    @Test
    public void testComments() throws Exception {
        Activities comments = getActivities().comments();
        assertThat(comments.size(), is(15));
    }

    @Test
    public void testGroupedByTrack() throws Exception {
        Map<Track,Activities> grouped = getActivities().groupedByTrack();
        assertThat(grouped.size(), is(19));
        for (Map.Entry<Track,Activities> entry : grouped.entrySet()) {
            assertThat(entry.getKey(), notNullValue());
            assertThat(entry.getValue().isEmpty(), is(false));
        }
    }

    @Test
    public void testOriginIsSetOnAllActivities() throws Exception {
        for (Activity e : getActivities()) {
            assertThat(e.origin, not(CoreMatchers.<Object>nullValue()));
        }
    }

    @Test
    public void testGetCursor() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getCursor(), is(nullValue()));
        activities.next_href = "http://foo.com?cursor=dada";
        assertThat(activities.getCursor(), equalTo("dada"));
    }

    private Activities getActivities() throws IOException {
        return Activities.fromJSON(getClass().getResourceAsStream("activities_all.json"));
    }

    @Test
    public void testSubTypes() throws Exception {
        for (Activity a : getActivities()) {
            assertTrue(a.origin.getClass().equals(a.type.typeClass));
        }
    }

    @Test
    public void testToJson() throws Exception {
        Activities initial = getActivities();
        assertThat(initial.future_href, not(nullValue()));
        assertThat(initial.next_href, not(CoreMatchers.<Object>nullValue()));

        String json = initial.toJSON();
        Activities other = Activities.fromJSON(json);
        for (int i=0; i<initial.size(); i++) {
            assertThat(initial.get(i), equalTo(other.get(i)));
        }
        assertThat(initial.size(), equalTo(other.size()));
        assertThat(initial.future_href, equalTo(other.future_href));
        assertThat(initial.next_href, equalTo(other.next_href));
    }

    @Test @Ignore
    // set to ignored because mutually exclusive annotation
    // @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public void testToJsonEqualsOriginal1() throws Exception {
        JsonNode original = AndroidCloudAPI.Mapper.readTree(getClass().getResourceAsStream("activities_all.json"));
        String json = Activities.fromJSON(getClass().getResourceAsStream("activities_all.json")).toJSON();
        JsonNode copy = AndroidCloudAPI.Mapper.readTree(json);
        assertThat(original, equalTo(copy));
    }

    @Test
    public void testToJsonEqualsOriginal2() throws Exception {
        JsonNode original = AndroidCloudAPI.Mapper.readTree(getClass().getResourceAsStream("incoming.json"));
        String json = Activities.fromJSON(getClass().getResourceAsStream("incoming.json")).toJSON();

        FileOutputStream fos = new FileOutputStream(new File("out.json"));
        fos.write(json.getBytes("UTF-8"));
        fos.close();

        JsonNode copy = AndroidCloudAPI.Mapper.readTree(json);
        assertThat(original, equalTo(copy));
    }


    @Test
    public void testMerge() throws Exception {
        Activities a1 = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        Activities a2 = Activities.fromJSON(getClass().getResourceAsStream("activities_2.json"));
        Activities all = Activities.fromJSON(getClass().getResourceAsStream("activities_all.json"));

        Activities merged = a2.merge(a1);
        assertThat(merged.size(), is(all.size()));

        assertThat(merged.future_href, equalTo("https://api.soundcloud.com/me/activities/tracks?uuid[to]=new_href"));
        assertThat(merged.next_href, equalTo("https://api.soundcloud.com/me/activities/tracks?cursor=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));
        assertTrue(merged.get(0).created_at.after(merged.get(merged.size()-1).created_at));
    }

    @Test
    public void testFilter() throws Exception {
        Activities a2 = Activities.fromJSON(getClass().getResourceAsStream("activities_2.json"));
        Date start = fromString("2011/07/29 15:36:44 +0000");

        Activities filtered = a2.filter(start);
        assertThat(filtered.size(), is(1));
        assertTrue(filtered.get(0).created_at.after(start));
    }

    @Test
    public void testGetNextRequest() throws Exception {
        Activities a1 = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        assertTrue(a1.hasMore());
        assertThat(a1.getNextRequest().toUrl(), equalTo("/me/activities/tracks?cursor=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));
    }

    @Test
    public void shouldFetchFromApi() throws Exception {
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class,
                "incoming_1.json",
                "incoming_2.json");

        Activities a = Activities.fetch(DefaultTestRunner.application, Content.ME_SOUND_STREAM.request(), -1);
        expect(a.size()).toEqual(100);
        expect(a.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
    }

    @Test
    public void shouldFetchOnlyUpToMaxItems() throws Exception {
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class,
                "incoming_1.json");

        Activities a = Activities.fetch(DefaultTestRunner.application,  Content.ME_SOUND_STREAM.request(), 20);
        expect(a.size()).toEqual(20);
        expect(a.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=future-href-incoming-1");
    }

    @Test
    public void shouldBuildContentValues() throws Exception {
        Activities a = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        ContentValues[] cv = a.buildContentValues();
        expect(cv.length).toEqual(a.size());
    }

    @Test
    public void shouldInsertActivitiesIntoDb() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));
        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        assertContentUriCount(Content.ME_SOUND_STREAM, 50);
    }

    @Test
    public void shouldGetActivitiesFromDB() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));
        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        assertContentUriCount(Content.ME_SOUND_STREAM, 50);

        expect(
            Activities.get(Content.ME_SOUND_STREAM,
            Robolectric.application.getContentResolver(), -1).size()
        ).toEqual(50);
    }

    @Test
    public void shouldGetActivitiesFromDBWithTimeFiltering() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));
        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        assertContentUriCount(Content.ME_SOUND_STREAM, 50);

        expect(
                Activities.get(Content.ME_SOUND_STREAM,
                        Robolectric.application.getContentResolver(),
                        toTime("2011/07/12 09:13:36 +0000")).size()
        ).toEqual(2);
    }

    @Test
    public void shouldClearAllActivities() throws Exception {
        Activities a = Activities.fromJSON(
                SyncAdapterServiceTest.class.getResourceAsStream("incoming_1.json"));

        a.insert(Content.ME_SOUND_STREAM, Robolectric.application.getContentResolver());
        assertContentUriCount(Content.ME_SOUND_STREAM, 50);

        LocalCollection.insertLocalCollection(Content.ME_SOUND_STREAM.uri,
                a.future_href,
                System.currentTimeMillis(), a.size(),
                Robolectric.application.getContentResolver());

        Activities.clear(null, Robolectric.application.getContentResolver());

        assertContentUriCount(Content.ME_SOUND_STREAM, 0);
        assertContentUriCount(Content.COLLECTIONS, 0);
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

        assertContentUriCount(Content.ME_ALL_ACTIVITIES, 4);

        Activities activities = Activities.get(Content.ME_ALL_ACTIVITIES, resolver, -1);
        expect(activities.size()).toEqual(4);

        Activity track = activities.get(2);
        expect(track.type).toEqual(Activity.Type.TRACK);
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
        expect(sharing.getTrack().id).toEqual(18676478L);
        expect(sharing.getTrack().permalink).toEqual("live-in-vegas");
        expect(sharing.getTrack().title).toEqual("Live in Vegas");
        expect(sharing.getTrack().sharing_note).not.toBeNull();
        expect(sharing.getTrack().sharing_note.text).toEqual("Enjoy, share, and dont be shy leave me your thoughts!");

        Activity comment = activities.get(1);
        expect(comment.type).toEqual(Activity.Type.COMMENT);
        expect(comment.getComment()).not.toBeNull();
        expect(comment.getComment().id).toEqual(22140210L);

        expect(comment.getTrack().id).toEqual(20023414L);
        expect(comment.getTrack().permalink).toEqual("sounds-from-dalston-kingsland");
        expect(comment.getTrack().title).toEqual("Sounds from Dalston Kingsland Railway Station (DLK)");
        expect(comment.getTrack().user_id).toEqual(133201L);
        expect(comment.getTrack().user.username).toEqual("Foo Bar");

        Activity favoriting = activities.get(0);

        expect(favoriting.type).toEqual(Activity.Type.FAVORITING);
        expect(favoriting.getFavoriting()).not.toBeNull();

        expect(favoriting.getFavoriting().track).toBe(favoriting.getTrack());

        expect(favoriting.getTrack().id).toEqual(13090155L);
        expect(favoriting.getTrack().permalink).toEqual("p-watzlawick-anleitung-zum");
        expect(favoriting.getTrack().title).toEqual("P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
        expect(favoriting.getTrack().user_id).toEqual(133201L);
        expect(favoriting.getTrack().user.username).toEqual("Foo Bar");
    }

}
