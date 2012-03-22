package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.task.FoursquareVenueTaskTest;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

@RunWith(DefaultTestRunner.class)
@DisableStrictI18n
public class LocationPickerTest {

    @After public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void shouldSetNameAndLocationAfterUserEntersText() throws Exception {
        TestHelper.addCannedResponses(FoursquareVenueTaskTest.class, "foursquare_venues.json");

        LocationPicker lp = new LocationPicker();
        Location loc = new Location(LocationManager.PASSIVE_PROVIDER);
        loc.setLatitude(12d);
        loc.setLongitude(13d);
        lp.setIntent(new Intent().putExtra(LocationPicker.EXTRA_LOCATION, loc));
        lp.onCreate(null);

        EditText text = (EditText) lp.findViewById(R.id.where);

        text.setText("Foo");
        Robolectric.shadowOf(text).triggerEditorAction(EditorInfo.IME_ACTION_DONE);

        expect(lp.isFinishing()).toBeTrue();
        Intent result = Robolectric.shadowOf(lp).getResultIntent();
        expect(result).not.toBeNull();
        expect(result.getData()).toEqual("location://manual");
        expect(result.getStringExtra(LocationPicker.EXTRA_NAME)).toEqual("Foo");
        expect(result.getDoubleExtra(LocationPicker.EXTRA_LATITUDE, -1d)).toEqual(12d);
        expect(result.getDoubleExtra(LocationPicker.EXTRA_LONGITUDE, -1d)).toEqual(13d);
    }

    @Test
    public void shouldSetFoursquareInformationAfterUserPicksVenue() throws Exception {

        TestHelper.addCannedResponses(FoursquareVenueTaskTest.class, "foursquare_venues.json");
        LocationPicker lp = new LocationPicker();
        lp.setIntent(new Intent().putExtra(LocationPicker.EXTRA_LOCATION,
                new Location(LocationManager.PASSIVE_PROVIDER)));
        lp.onCreate(null);

        expect(lp.getListAdapter().getCount()).toEqual(50);
        Robolectric.shadowOf(lp.getListView()).performItemClick(0);

        expect(lp.isFinishing()).toBeTrue();
        Intent result = Robolectric.shadowOf(lp).getResultIntent();
        expect(result).not.toBeNull();
        expect(result.getData()).toEqual("foursquare://venue/4adcda7ef964a520b74721e3");
        expect(result.getStringExtra(LocationPicker.EXTRA_NAME)).toEqual("U-Bhf Kottbusser Tor - U1, U8");
        expect(result.getDoubleExtra(LocationPicker.EXTRA_LATITUDE, -1d)).toEqual(0d);
        expect(result.getDoubleExtra(LocationPicker.EXTRA_LONGITUDE, -1d)).toEqual(0d);
    }

    @Test
    public void shouldAutomaticallyLoadVenuesIfLocationKnown() throws Exception {
        TestHelper.addCannedResponses(FoursquareVenueTaskTest.class, "foursquare_venues.json");
        LocationPicker lp = new LocationPicker();
        lp.setIntent(new Intent().putExtra(LocationPicker.EXTRA_LOCATION,
                new Location(LocationManager.PASSIVE_PROVIDER)));
        lp.onCreate(null);
        expect(lp.getListAdapter().getCount()).toEqual(50);
    }
}
