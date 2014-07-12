package com.soundcloud.android.creators.upload.tasks;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.FoursquareVenue;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.location.Location;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class FoursquareVenueTaskTest {

    @Test
    public void taskShouldReturnVenues() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "foursquare_venues.json");

        FoursquareVenueTask task = new FoursquareVenueTask();
        Location loc = new Location("mock");

        loc.setLongitude(52.499229);
        loc.setLatitude(13.418405);
        loc.setAccuracy(5);

        List<FoursquareVenue> venues = task.doInBackground(loc);
        expect(venues).not.toBeNull();
        expect(venues.size()).toEqual(50);

        FoursquareVenue kotti = venues.get(0);

        expect(kotti.name).toEqual("U-Bhf Kottbusser Tor - U1, U8");
        expect(kotti.id).toEqual("4adcda7ef964a520b74721e3");

        expect(kotti.categories).not.toBeNull();
        expect(kotti.categories.size()).toEqual(2);
        expect(kotti.categories.get(0).name).toEqual("Monument / Landmark");
        expect(kotti.categories.get(0).icon.toString()).toEqual("https://foursquare.com/img/categories/building/default.png");
        expect(kotti.categories.get(0).id).toEqual("4bf58dd8d48988d12d941735");
        expect(kotti.categories.get(0).primary).toBeTrue();
        expect(kotti.getCategory()).toEqual(kotti.categories.get(0));
        expect(kotti.getIcon().toString()).toEqual("https://foursquare.com/img/categories/building/default.png");
        expect(kotti.getHttpIcon().toString()).toEqual("http://foursquare.com/img/categories/building/default.png");
    }

    @Test
    public void taskShouldReturnNullWhenError() throws Exception {
        Robolectric.addPendingHttpResponse(400, "Error");
        FoursquareVenueTask task = new FoursquareVenueTask();
        Location loc = new Location("mock");
        expect(task.doInBackground(loc)).toBeNull();
    }

    @Test
    public void shouldDealWithEmptyResponses() throws Exception {
        String empty = "{\"meta\":{\"code\":200},\"response\":{\"groups\":[]}}";
        Robolectric.addPendingHttpResponse(200, empty);

        FoursquareVenueTask task = new FoursquareVenueTask();
        Location loc = new Location("mock");
        expect(task.doInBackground(loc).size()).toEqual(0);
    }

    @Test
    public void shouldSendVersionParameter() throws Exception {
        Robolectric.addPendingHttpResponse(200, "OK");
        Location loc = new Location("mock");
        FoursquareVenueTask task = new FoursquareVenueTask();
        task.doInBackground(loc);
        ((HttpGet)Robolectric.getSentHttpRequest(0)).getURI().getQuery().contains("v=20110601");
    }

}
