package com.soundcloud.android.analytics;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class ScreenTest {

    @Test
    public void shouldGetTrackingTag() {
        expect(Screen.EXPLORE_GENRES.get()).toEqual("explore:genres");
    }

    @Test
    public void shouldGetTrackingTagWithAppendedPath() {
        expect(Screen.EXPLORE_GENRES.get("path")).toEqual("explore:genres:path");
    }

    @Test
    public void gettingTagWithAppendedPathShouldNormalizePath() {
        expect(Screen.EXPLORE_GENRES.get("Hello & World")).toEqual("explore:genres:hello_&_world");
    }

    @Test
    public void setsAndGetsScreenFromIntent(){
        final Intent intent = new Intent();
        Screen.ACTIVITIES.addToIntent(intent);
        expect(Screen.fromIntent(intent)).toEqual(Screen.ACTIVITIES);
    }

    @Test
    public void setsAndGetsScreenFromBundle(){
        final Bundle bundle = new Bundle();
        Screen.ACTIVITIES.addToBundle(bundle);
        expect(Screen.fromBundle(bundle)).toEqual(Screen.ACTIVITIES);
    }

}
