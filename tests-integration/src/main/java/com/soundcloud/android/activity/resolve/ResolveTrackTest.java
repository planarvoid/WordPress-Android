package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.tests.ActivityTestCase;

import android.content.Intent;
import android.net.Uri;

public class ResolveTrackTest extends ActivityTestCase<ResolveActivity> {

    public ResolveTrackTest() {
        super(ResolveActivity.class);
    }

    public void testShouldLandOnLoginScreenForAnonymousUsers() {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        // We are not logged in

        assertThat(new HomeScreen(solo), is(Visible()));
        waiter.expect(solo.getToast()).toHaveText("Please log in to open this link");
    }

    private Uri getUri() {
        return TestConsts.CHE_FLUTE_URI;
    }
}
