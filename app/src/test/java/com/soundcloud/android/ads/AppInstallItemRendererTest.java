package com.soundcloud.android.ads;


import android.content.res.Resources;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Locale;

import static org.mockito.Mockito.verify;

public class AppInstallItemRendererTest extends AndroidUnitTest {

    private static final List<AppInstallAd> APP_INSTALLS = AdFixtures.getAppInstalls();
    private static final List<StreamItem> ITEMS = Lists.transform(APP_INSTALLS,
            new Function<AppInstallAd, StreamItem>() {
                @Nullable
                @Override
                public StreamItem apply(AppInstallAd ad) {
                    return StreamItem.forAppInstall(ad);
                }
            });

    @Mock private Resources resources;
    @Mock private ImageOperations imageOperations;
    @Mock private AppInstallItemRenderer.Listener listener;

    private AppInstallItemRenderer renderer;
    private View adView;

    @Before
    public void setUp() {
        final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
        renderer = new AppInstallItemRenderer(resources(), numberFormatter, imageOperations);
        renderer.setListener(listener);
        adView = renderer.createItemView(new FrameLayout(context()));
    }

    @Test
    public void onWhyAdsClickOpensDialog() {
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.why_ads).performClick();

        verify(listener).onWhyAdsClicked(adView.getContext());
    }

    @Test
    public void callToActionClickOpensClickThrough() {
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.call_to_action).performClick();

        verify(listener).onAppInstallItemClicked(adView.getContext(), APP_INSTALLS.get(0));
    }

    @Test
    public void imageClickOpensClickThrough() {
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.image).performClick();

        verify(listener).onAppInstallItemClicked(adView.getContext(), APP_INSTALLS.get(0));
    }

    @Test
    public void imageIsLoadedOnBind() {
        renderer.bindItemView(0, adView, ITEMS);

        final AppInstallAd ad = APP_INSTALLS.get(0);
        final ImageView imageView = (ImageView) adView.findViewById(R.id.image);

        verify(imageOperations).displayAppInstall(ad.getAdUrn(), ad.getImageUrl(), imageView);
    }
}
