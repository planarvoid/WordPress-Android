package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.model.FoursquareVenue;
import com.soundcloud.android.task.FoursquareVenueTask;
import com.soundcloud.android.utils.Capitalizer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

    public static final String EXTRA_LATITUDE  = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_NAME      = "name";
    public static final String EXTRA_4SQ_ID    = "id";
    public static final String EXTRA_LOCATION  = "location";
    public static final String EXTRA_VENUES    = "venues";

    private static final float MIN_ACCURACY = 60f; // stop updating when accuracy is MIN_ACCURACY meters
    private static final float MIN_DISTANCE = 10f; // get notified when location changes MIN_DISTANCE meters
    private static final long MIN_TIME = 5 * 1000; // request updates every 5sec

    private static final int LOADING = 0;

    private String mProvider;
    private Location mLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.location_picker);

        final FoursquareVenueAdapter adapter = new FoursquareVenueAdapter();
        final EditText where = (EditText) findViewById(R.id.where);
        where.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                     event.getAction() == KeyEvent.ACTION_DOWN)) {
                    Intent data = new Intent();

                    data.setData(Uri.parse("location://manual"));
                    data.putExtra(EXTRA_NAME, v.getText().toString());

                    if (mLocation != null) {
                        data.putExtra(EXTRA_LONGITUDE, mLocation.getLongitude());
                        data.putExtra(EXTRA_LATITUDE, mLocation.getLatitude());
                    }
                    setResult(RESULT_OK, data);
                    finish();
                }
                return true;
            }
        });
        where.addTextChangedListener(new Capitalizer(where));

        mProvider = getBestProvider(true);
        String alternativeProvider = getBestProvider(false);
        if (alternativeProvider != null && !alternativeProvider.equals(mProvider)) {
            // request updates in case provider gets enabled later
            requestLocationUpdates(alternativeProvider, adapter);
        }

        final Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(EXTRA_NAME)) where.setText(intent.getStringExtra(EXTRA_NAME));
            if (intent.hasExtra(EXTRA_LOCATION)) {
                mLocation = intent.getParcelableExtra(EXTRA_LOCATION);
            }
            if (intent.hasExtra(EXTRA_VENUES)) {
                ArrayList<FoursquareVenue> venues = intent.getParcelableArrayListExtra(EXTRA_VENUES);
                adapter.setVenues(venues);
            }
        }

        if (mLocation == null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "best provider: " + mProvider);
            mLocation = getManager().getLastKnownLocation(mProvider);
        }

        if (mLocation == null) {
            if (LocationManager.PASSIVE_PROVIDER.equals(mProvider)) {
                // no location & no location provider enabled, display warning
                new AlertDialog.Builder(this)
                        .setMessage(R.string.location_picker_no_providers_enabled)
                        .setPositiveButton(R.string.location_picker_go_to_settings, new Dialog.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                try {
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                } catch (ActivityNotFoundException ignored) {
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
                }
        } else {
            adapter.onLocationChanged(mLocation);
        }
        setListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mProvider = getBestProvider(true);
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
                progress.setMessage(getString(R.string.connect_progress));
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
        data.putExtra(EXTRA_4SQ_ID, venue.id);
        data.putExtra(EXTRA_NAME, venue.name);

        if (mLocation != null) {
            data.putExtra(EXTRA_LONGITUDE, mLocation.getLongitude());
            data.putExtra(EXTRA_LATITUDE, mLocation.getLatitude());
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
        final String provider = getManager().getBestProvider(c, enabled);
        return provider == null ? LocationManager.PASSIVE_PROVIDER : provider;
    }

    class FoursquareVenueAdapter extends BaseAdapter implements LocationListener {
        private List<FoursquareVenue> mVenues;

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
                if (mLocation != null &&
                   !mLocation.equals(location) &&
                    mLocation.distanceTo(location) < MIN_DISTANCE) {
                    // the preloaded location was good enough, stop here
                    getManager().removeUpdates(this);
                } else {
                    mLocation = location;
                    loadVenues(mLocation, FoursquareVenueTask.VENUE_LIMIT);

                    // stop requesting updates when we have a recent update with good accuracy
                    if (System.currentTimeMillis() - location.getTime() < 60 * 1000 &&
                            location.hasAccuracy() &&
                            location.getAccuracy() <= MIN_ACCURACY) {

                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "stop requesting updates, accuracy <= " + MIN_ACCURACY);
                        }
                        getManager().removeUpdates(this);
                    }
                }
            }
        }

        private void loadVenues(Location loc, int max) {
            if (loc == null) return;
            new FoursquareVenueTask(max) {
                @Override protected void onPreExecute() {
                    showDialog(LOADING);
                }
                @Override protected void onPostExecute(List<FoursquareVenue> venues) {
                    try {
                        dismissDialog(LOADING);
                    } catch (IllegalArgumentException ignored) {
                    }
                    setVenues(venues);
                }
            }.execute(loc);
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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Consts.OptionsMenu.REFRESH, 0, R.string.menu_load_more).setIcon(R.drawable.ic_menu_refresh);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.REFRESH:
                getListAdapter().loadVenues(mLocation, FoursquareVenueTask.VENUE_LIMIT_MAX);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
