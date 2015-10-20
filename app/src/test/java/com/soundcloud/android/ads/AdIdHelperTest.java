package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.schedulers.Schedulers;

// Had to use AndroidUnitTest because otherwise the observeOn(mainThread) isn't working
public class AdIdHelperTest extends AndroidUnitTest {

    @Mock private AdIdWrapper adIdWrapper;

    private AdIdHelper adIdHelper;

    @Before
    public void setUp() throws Exception {
        adIdHelper = new AdIdHelper(adIdWrapper, Schedulers.immediate());

        when(adIdWrapper.isPlayServicesAvailable()).thenReturn(true);
        when(adIdWrapper.getAdInfo()).thenReturn(new AdvertisingIdClient.Info("my-adid", false));
    }

    @Test
    public void adIdIsLoadedIfPlayServicesIsAvailable() {
        adIdHelper.init();

        assertThat(adIdHelper.getAdId()).isEqualTo(Optional.of("my-adid"));
    }

    @Test
    public void adIdIsNotLoadedIfPlayServicesIsUnavailable() {
        when(adIdWrapper.isPlayServicesAvailable()).thenReturn(false);

        adIdHelper.init();

        assertThat(adIdHelper.getAdId().isPresent()).isFalse();
    }

    @Test
    public void googleTrackingBooleanIsInvertedToMatchAppleEquivalent() {
        adIdHelper.init();

        assertThat(adIdHelper.getAdId().isPresent()).isTrue();
        assertThat(adIdHelper.getAdIdTracking()).isTrue();
    }

    @Test
    public void adIdIsNotAvailableUntilLoaded() {
        assertThat(adIdHelper.getAdId().isPresent()).isFalse();
    }

    @Test
    public void adIdIsNotAvailableOnError() throws Exception {
        when(adIdWrapper.getAdInfo()).thenThrow(new GooglePlayServicesNotAvailableException(123));

        adIdHelper.init();

        assertThat(adIdHelper.getAdId().isPresent()).isFalse();
    }
    
}
