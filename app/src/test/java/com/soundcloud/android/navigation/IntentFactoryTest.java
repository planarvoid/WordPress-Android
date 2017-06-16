package com.soundcloud.android.navigation;

import static com.soundcloud.android.model.Urn.forAd;

import com.soundcloud.android.ads.FullScreenVideoActivity;
import com.soundcloud.android.ads.PrestitialActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.assertions.IntentAssert;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentFactoryTest extends AndroidUnitTest {

    @Mock Context context;

    @Test
    public void openAdClickthrough() {
        final Uri uri = Uri.parse("http://clickthroughurl.com");
        assertIntent(IntentFactory.createAdClickthroughIntent(uri))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(uri)
                .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void openVideoFullScreen() {
        final Urn urn = forAd("network", "123");
        assertIntent(IntentFactory.createFullscreenVideoAdIntent(context, urn))
                .containsExtra(FullScreenVideoActivity.EXTRA_AD_URN, urn)
                .opensActivity(FullScreenVideoActivity.class);
    }

    @Test
    public void openVisualPrestitial() {
        assertIntent(IntentFactory.createPrestititalAdIntent(context))
                .opensActivity(PrestitialActivity.class);
    }

    private IntentAssert assertIntent(Intent intent) {
        return new IntentAssert(intent);
    }
}
