package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.hamcrest.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
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

    public void test_should_land_on_login_screen_for_anonymous_users() {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(getUri()));
        // We are not logged in

        assertThat(new HomeScreen(solo), is(Visible()));
        assertThat(solo.getToast().getText(), is(equalToIgnoringCase("Please log in to open this link")));
    }

    private Uri getUri() {
        return TestConsts.CHE_FLUTE_URI;
    }
}
