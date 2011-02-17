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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.utils.http.Http;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;


public class LocationPicker extends ListActivity {
    public static final int PICK_VENUE = 9003;     // Intent request code

    private static final float MIN_ACCURACY = 60f; // stop updating when accuracy is MIN_ACCURACY meters
    private static final float MIN_DISTANCE = 10f; // get notified when location changes MIN_DISTANCE meters
    private static final long MIN_TIME = 5 * 1000; // request updates every 5sec
    private static final int VENUE_LIMIT     = 25; // fetch this number of 4sq venues

    private static final int LOADING = 0;

    private String provider;

    private LocationManager getManager() {
        return (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.location_picker);

        final EditText where = (EditText) findViewById(R.id.where);

        where.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Intent data = new Intent();

                    data.setData(Uri.parse("location://manual"));
                    data.putExtra("name", v.getText().toString());

                    setResult(RESULT_OK, data);
                    finish();
                }
                return true;
            }
        });

        where.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.length() == 1 &&
                        !s.toString().toUpperCase().contentEquals(s.toString())) {
                    where.setTextKeepState(s.toString().toUpperCase());
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        if (getIntent().hasExtra("name")) where.setText(getIntent().getStringExtra("name"));

        Criteria c = new Criteria();
        this.provider = getManager().getBestProvider(c, true);
        Log.v(TAG, "best provider: " + provider);

        FoursquareVenueAdapter adapter = new FoursquareVenueAdapter();
        Location loc = getManager().getLastKnownLocation(provider);
        adapter.onLocationChanged(loc);
        setListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (provider != null) {
            getManager().requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, (LocationListener) getListAdapter());
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
        // XXX replace with official SoundCloud API keys
        public static final String client_id = "AOJ23GCHGN2C5OWX4OYLWMXKBKAKPS2BK3VE122CEACTY1KD";
        public static final String client_secret = "BZ0MCMCYMUWMB1DPAJI40BLV3YTSQB4XRQKSR1ZMRM442F3R";
        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        protected List<Venue> doInBackground(Location... locations) {
            Location loc = locations[0];
            HttpClient client = new DefaultHttpClient();
            HttpHost host = new HttpHost("api.foursquare.com", -1, "https");

            final String ll = String.format("%.6f,%.6f", loc.getLatitude(), loc.getLongitude());
            //http://developer.foursquare.com/docs/venues/search.html
            Http.Params p = new Http.Params(
                    "ll",            ll,
                    "limit",         VENUE_LIMIT,
                    "client_id",     client_id,
                    "client_secret", client_secret);

            if (loc.hasAccuracy()) p.add("llAcc", loc.getAccuracy());

            HttpGet request = new HttpGet("/v2/venues/search?" + p);
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
        public List<Category> categories;

        public Category getCategory() {
            if (categories == null || categories.size() == 0) return null;
            for (Category c : categories) if (c.primary) return c;
            return null;
        }

        public URI getIcon() {
            Category c = getCategory();
            return c == null ? null : c.icon;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Category {
            public String id, name;
            public boolean primary;
            public URI icon;
        }
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
            View view;
            if (convertView == null) {
                LayoutInflater inf = (LayoutInflater)
                        parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inf.inflate(R.layout.location_picker_row, parent, false);
            } else {
                view = convertView;
            }

            TextView name = (TextView) view.findViewById(R.id.venue_name);
            ImageView image = (ImageView) view.findViewById(R.id.venue_category_icon);

            Venue venue = getItem(position);

            URI categoryIcon = venue.getIcon();
            if (categoryIcon != null) {
                try {
                    ImageLoader.get(parent.getContext()).bind(
                            this,
                            image,
                            categoryIcon.toString());
                } catch (Exception e) {
                    Log.e(TAG, "error", e);
                }
            }
            name.setText(venue.name);
            return view;
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

                // stop requesting updates when we have a recent update with good accuracy
                if (System.currentTimeMillis() - location.getTime() < 60 * 1000 &&
                        location.hasAccuracy() &&
                        location.getAccuracy() <= MIN_ACCURACY) {
                    Log.d(TAG, "stop requesting updates, accuracy <= " + MIN_ACCURACY);
                    getManager().removeUpdates(this);
                }
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
