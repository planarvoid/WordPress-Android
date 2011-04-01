package com.soundcloud.android.activity;

import android.location.Location;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Http;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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


    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    public static void main(String[] args) throws Exception {

        HttpClient client = new DefaultHttpClient();
        HttpHost host = new HttpHost("api.foursquare.com", -1, "https");

        //http://developer.foursquare.com/docs/venues/search.html
        HttpGet request = new HttpGet("/v2/venues/search?" + new Http.Params(
                "ll", "52.499229,13.418405",
                "llAcc", 5,
                "limit", 50,
                "client_id", LocationPicker.FoursquareApiTask.client_id,
                "client_secret", LocationPicker.FoursquareApiTask.client_secret
        ));

        HttpResponse resp = client.execute(host, request);
        if (resp.getStatusLine().getStatusCode() == 200) {
            byte[] buffer = new byte[8192];
            InputStream content = resp.getEntity().getContent();

            System.out.println();
            int n;
            while ((n = content.read(buffer)) != -1) {
                System.out.print(new String(buffer, 0, n));
            }
            System.out.println();


        } else {
            System.err.println(resp.getStatusLine());
        }
    }
}
