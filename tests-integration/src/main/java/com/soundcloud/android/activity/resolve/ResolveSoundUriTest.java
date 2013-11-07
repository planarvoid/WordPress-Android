package com.soundcloud.android.activity.resolve;

import android.net.Uri;
import com.soundcloud.android.TestConsts;


public class ResolveSoundUriTest extends ResolveTrackTest {
    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_SOUND_URI;
    }
}
