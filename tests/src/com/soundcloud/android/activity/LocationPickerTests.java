package com.soundcloud.android.activity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.location.Location;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class LocationPickerTests {

    private String slurp(String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = getClass().getResourceAsStream(res);
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }

    @Test
    public void taskShouldReturnVenues() throws Exception {
        Robolectric.addPendingHttpResponse(200, slurp("foursquare_venues.json"));

        LocationPicker.FoursquareApiTask task = new LocationPicker.FoursquareApiTask();
        Location loc = new Location("mock");

        loc.setLongitude(52.499229);
        loc.setLatitude(13.418405);
        loc.setAccuracy(5);

        List<LocationPicker.Venue> venues = task.doInBackground(loc);
        assertNotNull(venues);
        assertThat(venues.size(), equalTo(50));

        LocationPicker.Venue kotti = venues.get(0);

        assertThat(kotti.name, equalTo("U-Bhf Kottbusser Tor - U1, U8"));
        assertThat(kotti.id, equalTo("4adcda7ef964a520b74721e3"));

        assertNotNull(kotti.categories);
        assertThat(kotti.categories.size(), equalTo(2));
        assertThat(kotti.categories.get(0).name, equalTo("Monument / Landmark"));
        assertThat(kotti.categories.get(0).icon.toString(), equalTo("http://foursquare.com/img/categories/building/default.png"));
        assertThat(kotti.categories.get(0).id, equalTo("4bf58dd8d48988d12d941735"));
        assertThat(kotti.categories.get(0).primary, is(true));
        assertThat(kotti.getCategory(), equalTo(kotti.categories.get(0)));
        assertThat(kotti.getIcon().toString(), equalTo("http://foursquare.com/img/categories/building/default.png"));
    }

    @Test
    public void taskShouldReturnNullWhenError() throws Exception {
        Robolectric.addPendingHttpResponse(400, "Error");
        LocationPicker.FoursquareApiTask task = new LocationPicker.FoursquareApiTask();
        Location loc = new Location("mock");
        assertNull(task.doInBackground(loc));
    }

    @Test
    public void shouldDealWithEmptyResponses() throws Exception {
        String empty = "{\"meta\":{\"code\":200},\"response\":{\"groups\":[]}}";
        Robolectric.addPendingHttpResponse(200, empty);

        LocationPicker.FoursquareApiTask task = new LocationPicker.FoursquareApiTask();
        Location loc = new Location("mock");
        assertThat(task.doInBackground(loc).size(), equalTo(0));
    }
}
