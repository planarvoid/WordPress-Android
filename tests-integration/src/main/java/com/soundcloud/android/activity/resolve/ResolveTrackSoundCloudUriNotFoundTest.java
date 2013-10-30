package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.TestConsts;

import android.net.Uri;

public class ResolveTrackSoundCloudUriNotFoundTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.UNRESOLVABLE_SC_TRACK_URI;
    }

    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() throws Exception {
        solo.assertText(R.string.error_loading_url, DEFAULT_WAIT);
    }
}