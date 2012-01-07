package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Expect;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.codehaus.jackson.JsonNode;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        Activities activities = new Activities(new Event(), new Event());
        assertThat(activities.isEmpty(), is(false));
        assertThat(activities.size(), is(2));
    }

    @Test
    public void testGetUniqueUsers() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getUniqueUsers().size(), is(0));
        Event e1 = new Event() { public User getUser() { return new User() { { id = 1; } }; } };
        Event e2 = new Event() { public User getUser() { return new User() { { id = 1; } }; } };
        Event e3 = new Event() { public User getUser() { return new User() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        assertThat(activities.getUniqueUsers().size(), is(2));
    }

    @Test
    public void testGetUniqueTracks() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getUniqueTracks().size(), is(0));
        Event e1 = new Event() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Event e2 = new Event() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Event e3 = new Event() { public Track getTrack() { return new Track() { { id = 3; } }; } };
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
        for (Event e : getActivities()) {
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
        return Activities.fromJSON(getClass().getResourceAsStream("activities.json"));
    }

    @Test
    public void testSubTypes() throws Exception {
        for (Event a : getActivities()) {
            assertTrue(a.origin.getClass().equals(a.getOriginClass()));
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
        JsonNode original = AndroidCloudAPI.Mapper.readTree(getClass().getResourceAsStream("activities.json"));
        String json = Activities.fromJSON(getClass().getResourceAsStream("activities.json")).toJSON();
        JsonNode copy = AndroidCloudAPI.Mapper.readTree(json);
        assertThat(original, equalTo(copy));
    }

    @Test
    public void testToJsonEqualsOriginal2() throws Exception {
        JsonNode original = AndroidCloudAPI.Mapper.readTree(getClass().getResourceAsStream("incoming_1.json"));
        String json = Activities.fromJSON(getClass().getResourceAsStream("incoming_1.json")).toJSON();

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
        Activities all = Activities.fromJSON(getClass().getResourceAsStream("activities.json"));

        Activities merged = a2.merge(a1);
        assertThat(merged.size(), is(all.size()));

        assertThat(merged.future_href, equalTo("https://api.soundcloud.com/me/activities/tracks?uuid[to]=new_href"));
        assertThat(merged.next_href, equalTo("https://api.soundcloud.com/me/activities/tracks?cursor=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));
        assertTrue(merged.get(0).created_at.after(merged.get(merged.size()-1).created_at));
    }

    @Test
    public void testTrimBelow() throws Exception {
        Activities a1 = Activities.fromJSON(getClass().getResourceAsStream("activities_1.json"));
        Activities a2 = Activities.fromJSON(getClass().getResourceAsStream("activities_2.json"));

        expect(a1.size()).toEqual(39);
        expect(a2.size()).toEqual(3);

        Activities trimmed = a1.merge(a2).trimBelow(10);

        expect(trimmed.size()).toEqual(0);
        expect(trimmed.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738");
        expect(trimmed.next_href).toBeNull();

        trimmed = a1.merge(a2).trimBelow(40);

        expect(trimmed.size()).toEqual(39);
        expect(trimmed.future_href).toEqual("https://api.soundcloud.com/me/activities/tracks?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738");
        expect(trimmed.next_href).toEqual("https://api.soundcloud.com/me/activities/tracks?cursor=e46666c4-a7e6-11e0-8c30-73a2e4b61738");
    }


    @Test
    public void testFilter() throws Exception {
        Activities a2 = Activities.fromJSON(getClass().getResourceAsStream("activities_2.json"));
        Date start = AndroidCloudAPI.CloudDateFormat.fromString("2011/07/29 15:36:44 +0000");

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
}
