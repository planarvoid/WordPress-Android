package com.soundcloud.android.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class ActitiviesTest {

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
        Event e1 = new Event() {{ user = new User() { { id = 1; } }; } };
        Event e2 = new Event() {{ user = new User() { { id = 1; } }; } };
        Event e3 = new Event() {{ user = new User() { { id = 2; } }; } };
        activities = new Activities(e1, e2, e3);
        assertThat(activities.getUniqueUsers().size(), is(2));
    }

    @Test
    public void testGetUniqueTracks() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getUniqueTracks().size(), is(0));
        Event e1 = new Event() {{ }};
        Event e2 = new Event();
        Event e3 = new Event();
        e1.track = new Track() { { id = 1; } };
        e2.track = new Track() { { id = 1; } };
        e3.track = new Track() { { id = 2; } };
        activities = new Activities(e1, e2, e3);
        assertThat(activities.getUniqueTracks().size(), is(2));
    }

    @Test
    public void testFromJSON() throws Exception {
        Activities a = getActivities();
        assertThat(a.size(), is(42));
        assertThat(a.getUniqueTracks().size(), is(13));
        assertThat(a.getUniqueUsers().size(), is(29));
    }

    @Test
    public void testFavoritings() throws Exception {
        Activities favoritings = getActivities().favoritings();
        assertThat(favoritings.size(), is(27));
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
            //System.err.println("track: "+entry.getKey()+ "==>"+entry.getValue());
        }
    }

    private Activities getActivities() throws IOException {
        return Activities.fromJSON(getClass().getResourceAsStream("activities.json"));
    }
}
