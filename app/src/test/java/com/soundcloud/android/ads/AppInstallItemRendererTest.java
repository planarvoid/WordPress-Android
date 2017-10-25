package com.soundcloud.android.ads;


import static com.soundcloud.android.events.AdPlaybackEvent.InlayAdEvent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.LoadingState;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Observable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppInstallItemRendererTest extends AndroidUnitTest {

    private static final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private static final Date CURRENT_DATE = new Date();
    private static final List<AppInstallAd> APP_INSTALLS = AdFixtures.getAppInstalls();
    private static final List<StreamItem> ITEMS = Lists.transform(APP_INSTALLS, StreamItem.AppInstall::new);

    @Mock private Resources resources;
    @Mock private ImageOperations imageOperations;
    @Mock private AppInstallItemRenderer.Listener listener;
    @Mock private EventBus eventBus;
    private CurrentDateProvider dateProvider = new TestDateProvider(CURRENT_DATE.getTime());

    private AppInstallItemRenderer renderer;
    private View adView;

    @Before
    public void setUp() {
        renderer = new AppInstallItemRenderer(resources(), numberFormatter, imageOperations, dateProvider, eventBus);
        renderer.setListener(listener);
        adView = renderer.createItemView(new FrameLayout(context()));
        when(imageOperations.displayAdImage(any(Urn.class), any(String.class), any(ImageView.class))).thenReturn(Observable.just(new LoadingState.Complete(null, null, null)));
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

        verify(listener).onAdItemClicked(APP_INSTALLS.get(0));
    }

    @Test
    public void imageClickOpensClickThrough() {
        renderer.bindItemView(0, adView, ITEMS);

        adView.findViewById(R.id.image).performClick();

        verify(listener).onAdItemClicked(APP_INSTALLS.get(0));
    }

    @Test
    public void imageIsLoadedOnBindAndRegistersAdsImageLoadListener() {
        renderer.bindItemView(0, adView, ITEMS);

        final AppInstallAd ad = APP_INSTALLS.get(0);
        final ImageView imageView = adView.findViewById(R.id.image);

        verify(imageOperations).displayAdImage(
                eq(ad.adUrn()),
                eq(ad.imageUrl()),
                eq(imageView)
        );
    }

    @Test
    public void imageLoadTimeListenerSetsImageLoadTimeAndPublishesImageLoadedEvent() {
        renderer.bindItemView(0, adView, ITEMS);
        final AppInstallAd ad = APP_INSTALLS.get(0);

        assertThat(ad.imageLoadTime()).isEqualTo(Optional.of(CURRENT_DATE));

        verify(eventBus).publish(EventQueue.AD_PLAYBACK, InlayAdEvent.forImageLoaded(0, ad, CURRENT_DATE));
    }

    abstract class RunnableRenderer extends AppInstallItemRenderer implements Runnable {
        RunnableRenderer(Resources resources,
                         CondensedNumberFormatter numberFormatter,
                         ImageOperations imageOperations,
                         CurrentDateProvider dateProvider,
                         EventBus eventBus) {
            super(resources, numberFormatter, imageOperations, dateProvider, eventBus);
        }
    }
}
