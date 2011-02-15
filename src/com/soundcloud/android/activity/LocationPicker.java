package com.soundcloud.android.activity;

import android.app.ListActivity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LocationPicker extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FoursquareVenueAdapter adapter = new FoursquareVenueAdapter();

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria c = new Criteria();
        String provider = manager.getBestProvider(c, true);

        if (provider != null) {
            Log.d(TAG, "best provider: " + provider);
            manager.requestLocationUpdates(provider, 1000, 5, adapter);
        }

        Location loc = manager.getLastKnownLocation(provider);
        adapter.onLocationChanged(loc);

        setListAdapter(adapter);
    }



    static class FoursquareVenueAdapter extends BaseAdapter implements LocationListener {
        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged("+location+")");
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
