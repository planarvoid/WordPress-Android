package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

@RunWith(DefaultTestRunner.class)
public class LocationPickerTest {
    @Test
    public void shouldSetNameAndLocationAfterUserEntersText() throws Exception {
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
}
