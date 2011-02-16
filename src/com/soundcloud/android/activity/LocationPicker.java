package com.soundcloud.android.activity;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.task.AsyncApiTask.queryString;


public class LocationPicker extends ListActivity {
    public static final int PICK_VENUE = 9003;
    private static final int LOADING = 0;

    private String provider;

    private LocationManager getManager() {
        return (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FoursquareVenueAdapter adapter = new FoursquareVenueAdapter();

        Criteria c = new Criteria();
        this.provider = getManager().getBestProvider(c, true);
        Log.v(TAG, "best provider: " + provider);

        Location loc = getManager().getLastKnownLocation(provider);
        adapter.onLocationChanged(loc);

        setListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (provider != null) {
            getManager().requestLocationUpdates(provider, 1000, 5, (LocationListener) getListAdapter());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getManager().removeUpdates((LocationListener) getListAdapter());
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case LOADING:
                ProgressDialog progress = new ProgressDialog(this);
                progress.setMessage("Loading");
                progress.setCancelable(true);
                progress.setIndeterminate(true);
                return progress;
            default:
                return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        FoursquareVenueAdapter adapter = (FoursquareVenueAdapter) getListAdapter();
        Venue venue = adapter.getItem(position);

        Intent data = new Intent();
        data.setData(Uri.parse("foursquare://venue/" + venue.id));
        data.putExtra("id", venue.id);
        data.putExtra("name", venue.name);
        data.putExtra("longitude", adapter.getLocation().getLongitude());
        data.putExtra("latitude", adapter.getLocation().getLatitude());

        setResult(RESULT_OK, data);
        finish();
    }

    static class FoursquareApiTask extends AsyncTask<Location, Integer, List<Venue>> {
        public static final String client_id = "AOJ23GCHGN2C5OWX4OYLWMXKBKAKPS2BK3VE122CEACTY1KD";
        public static final String client_secret = "BZ0MCMCYMUWMB1DPAJI40BLV3YTSQB4XRQKSR1ZMRM442F3R";
        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        protected List<Venue> doInBackground(Location... params) {
            Location loc = params[0];
            HttpClient client = new DefaultHttpClient();
            HttpHost host = new HttpHost("api.foursquare.com", -1, "https");

            //http://developer.foursquare.com/docs/venues/search.html
            HttpGet request = new HttpGet("/v2/venues/search?" + queryString(
                    "ll", String.format("%.6f,%.6f", loc.getLongitude(), loc.getLatitude()),
                    "limit", 50,
                    "client_id", client_id,
                    "client_secret", client_secret
            ));

            try {
                HttpResponse resp = client.execute(host, request);
                switch (resp.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_OK:
                        JsonNode root = mapper.readTree(resp.getEntity().getContent());
                        JsonNode groups = root.get("response").get("groups");
                        if (groups.size() == 0) {
                            return new ArrayList<Venue>();
                        } else {
                            JsonNode items = groups.get(0).get("items");
                            return mapper.readValue(items, TypeFactory.collectionType(ArrayList.class, Venue.class));
                        }
                    default:
                        Log.e(TAG, "unexpected status code: " + resp.getStatusLine());
                        return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "error", e);
                return null;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Venue {
        public String id, name;
    }

    class FoursquareVenueAdapter extends BaseAdapter implements LocationListener {
        private List<Venue> venues;
        private Location location;

        @Override
        public int getCount() {
            return venues == null ? 0 : venues.size();
        }

        @Override
        public Venue getItem(int position) {
            return venues.get(position);
        }

        @Override
        public long getItemId(int position) {
            return venues.get(position).id.hashCode();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                ((TextView) convertView).setText(getItem(position).name);
                return convertView;
            } else {
                TextView view = new TextView(parent.getContext());
                view.setText(getItem(position).name);
                view.setTextSize(20f);
                return view;
            }
        }

        public void setVenues(List<Venue> venues) {
            this.venues = venues;
            notifyDataSetChanged();
        }

        public Location getLocation() {
            return location;
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged(" + location + ")");
            if (location != null) {
                this.location = location;
                new FoursquareApiTask() {
                    @Override
                    protected void onPreExecute() {
                        showDialog(LOADING);
                    }

                    @Override
                    protected void onPostExecute(List<Venue> venues) {
                        try {
                            dismissDialog(LOADING);
                        } catch (IllegalArgumentException ignored) {
                        }

                        setVenues(venues);
                    }
                }.execute(location);
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    }
}
