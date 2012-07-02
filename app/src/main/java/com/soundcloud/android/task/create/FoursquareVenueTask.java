package com.soundcloud.android.task.create;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.model.FoursquareVenue;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoursquareVenueTask extends AsyncTask<Location, Void, List<FoursquareVenue>> {
    public static final int VENUE_LIMIT     = 25; // fetch this number of 4sq venues
    public static final int VENUE_LIMIT_MAX = 50; // max supported by 4sq API

    // registered w/ hannes@soundcloud.com
    public static final String client_id     = "KO0RS1BR5VCXT4CR2GRCYA1Z2KSMM3QJVWJ35V2CVBUWFYWP";
    public static final String client_secret = "MDAXDKVZRURKHDBRSW0KKTL4NNLQW1WEKUM2IDHELZKPJRWI";
    private static final ObjectMapper mapper = new ObjectMapper();
    private int mVenueLimit;

    public FoursquareVenueTask() {
        this(VENUE_LIMIT);
    }

    public FoursquareVenueTask(int venuelimit) {
        mVenueLimit = venuelimit;
    }

    @Override
    protected List<FoursquareVenue> doInBackground(Location... locations) {
        Location loc = locations[0];
        HttpClient client = HttpUtils.createHttpClient(CloudAPI.USER_AGENT);
        final String ll = String.format(Locale.ENGLISH, "%.6f,%.6f", loc.getLatitude(), loc.getLongitude());

        //http://developer.foursquare.com/docs/venues/search.html
        Request r = new Request("https://api.foursquare.com/v2/venues/search").with(
                "ll",            ll,
                "limit",         mVenueLimit,
                "client_id",     client_id,
                "client_secret", client_secret);

        if (loc.hasAccuracy()) r.add("llAcc", loc.getAccuracy());

        try {
            HttpResponse resp = client.execute(r.buildRequest(HttpGet.class));
            switch (resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                    JsonNode root = mapper.readTree(resp.getEntity().getContent());
                    JsonNode groups = root.get("response").get("groups");
                    if (groups != null) {
                        JsonNode nearby = null;
                        for (JsonNode g : groups) {
                            if (g.get("type") == null) continue;
                            if ("nearby".equals(g.get("type").asText())) {
                                nearby = g;
                                break;
                            }
                        }
                        if (nearby != null) {
                            JsonNode items = nearby.get("items");
                            return mapper.readValue(items.traverse(),
                                    mapper.getTypeFactory().constructCollectionType(ArrayList.class, FoursquareVenue.class));
                        }
                    }
                    return new ArrayList<FoursquareVenue>();
                default:
                    Log.e(TAG, "unexpected status code: " + resp.getStatusLine());
                    return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return null;
        } finally {
            HttpUtils.closeHttpClient(client);
        }
    }
}
