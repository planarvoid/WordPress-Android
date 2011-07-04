package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.FoursquareVenue;
import com.soundcloud.android.task.FoursquareVenueTask;
import com.soundcloud.android.utils.Capitalizer;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class LocationPicker extends ListActivity {
    public static final int PICK_VENUE = 9003;     // Intent request code

    private static final float MIN_ACCURACY = 60f; // stop updating when accuracy is MIN_ACCURACY meters
    private static final float MIN_DISTANCE = 10f; // get notified when location changes MIN_DISTANCE meters
    private static final long MIN_TIME = 5 * 1000; // request updates every 5sec

    private static final int LOADING = 0;

    private String mProvider;
    private Location mPreloadedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.location_picker);

        final EditText where = (EditText) findViewById(R.id.where);

        where.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                     event.getAction() == KeyEvent.ACTION_DOWN)) {
                    Intent data = new Intent();

                    data.setData(Uri.parse("location://manual"));
                    data.putExtra("name", v.getText().toString());

                    setResult(RESULT_OK, data);
                    finish();
                }
                return true;
            }
        });

        where.addTextChangedListener(new Capitalizer(where));

        final FoursquareVenueAdapter adapter = new FoursquareVenueAdapter();
        final Intent intent = getIntent();

        if (intent.hasExtra("name")) where.setText(intent.getStringExtra("name"));
        if (intent.hasExtra("location")) mPreloadedLocation = intent.getParcelableExtra("location");

        mProvider = getBestProvider(true);
        String alternativeProvider = getBestProvider(false);
        if (alternativeProvider != null &&
            !alternativeProvider.equals(mProvider)) {
            // request updates in case provider gets enabled later
            requestLocationUpdates(alternativeProvider, adapter);
        }

        if (intent.hasExtra("venues")) {
            ArrayList<FoursquareVenue> venues =
                    intent.getParcelableArrayListExtra("venues");

            if (!venues.isEmpty()) adapter.setVenues(venues);
            adapter.setLocation(mPreloadedLocation);
        } else if  (mProvider != null) {
            Log.v(TAG, "best provider: " + mProvider);
            Location loc = getManager().getLastKnownLocation(mProvider);
            adapter.onLocationChanged(loc);
        }

        setListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates(mProvider, getListAdapter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        getManager().removeUpdates(getListAdapter());
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
        FoursquareVenueAdapter adapter = getListAdapter();
        FoursquareVenue venue = adapter.getItem(position);

        Intent data = new Intent();
        data.setData(Uri.parse("foursquare://venue/" + venue.id));
        data.putExtra("id", venue.id);
        data.putExtra("name", venue.name);

        final Location loc = adapter.getLocation();
        if (loc != null) {
            data.putExtra("longitude", loc.getLongitude());
            data.putExtra("latitude", loc.getLatitude());
        }
        setResult(RESULT_OK, data);
        finish();
    }

    private LocationManager getManager() {
        return (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void requestLocationUpdates(String provider, LocationListener listener) {
        if (provider != null) {
            getManager().requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, listener);
        }
    }

    @Override
    public FoursquareVenueAdapter getListAdapter() {
        return (FoursquareVenueAdapter) super.getListAdapter();
    }

    private String getBestProvider(boolean enabled) {
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        return getManager().getBestProvider(c, enabled);
    }

    class FoursquareVenueAdapter extends BaseAdapter implements LocationListener {
        private List<FoursquareVenue> mVenues;
        private Location mLocation;

        @Override
        public int getCount() {
            return mVenues == null ? 0 : mVenues.size();
        }

        @Override
        public FoursquareVenue getItem(int position) {
            return mVenues.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mVenues.get(position).id.hashCode();
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

            FoursquareVenue venue = getItem(position);
            URI categoryIcon = venue.getHttpIcon(); // Android has problems with HTTPS
            if (categoryIcon != null) {
                try {
                    ImageLoader.BindResult result = ImageLoader.get(parent.getContext()).bind(
                            this,
                            image,
                            categoryIcon.toString());
                    if (result == ImageLoader.BindResult.ERROR) {
                        Log.e(TAG, "error loading");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error", e);
                }
            }
            name.setText(venue.name);
            return view;
        }

        public void setVenues(List<FoursquareVenue> venues) {
            this.mVenues = venues;
            notifyDataSetChanged();
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                if (mPreloadedLocation != null &&
                    mPreloadedLocation.distanceTo(location) < MIN_DISTANCE) {
                    // the preloaded location was good enough, stop here
                    getManager().removeUpdates(this);
                    return;
                }

                this.mLocation = location;
                new FoursquareVenueTask() {
                    @Override
                    protected void onPreExecute() {
                        showDialog(LOADING);
                    }

                    @Override
                    protected void onPostExecute(List<FoursquareVenue> venues) {
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
        public void onStatusChanged(String name, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String name) {
            LocationProvider provider = getManager().getProvider(name);
            if (provider != null && mProvider != null &&
                getManager().getProvider(mProvider).getAccuracy() >
                provider.getAccuracy()) {
                // this provider is better, use it
                requestLocationUpdates(name, this);
                mProvider = name;
            }
        }

        @Override
        public void onProviderDisabled(String name) {
            if (name.equals(mProvider)) {
                mProvider = getBestProvider(true);
                requestLocationUpdates(mProvider, this);
            }
        }

        public Location getLocation() {
            return mLocation;
        }

        public void setLocation(Location location) {
            mLocation = location;
        }
    }
}
