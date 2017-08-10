package com.soundcloud.android.ads;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

public class AdIdHelperTest extends AndroidUnitTest {

    @Mock private AdIdWrapper adIdWrapper;

    private AdIdHelper adIdHelper;

    @Before
    public void setUp() throws Exception {
        adIdHelper = new AdIdHelper(adIdWrapper, Schedulers.trampoline());

        when(adIdWrapper.isPlayServicesAvailable()).thenReturn(true);
        when(adIdWrapper.getAdInfo()).thenReturn(Optional.of(new AdvertisingIdClient.Info("my-adid", false)));
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

    @Test
    public void adIdIsNotLoadedIfAdInfoContainsNullId() throws GooglePlayServicesNotAvailableException, IOException, GooglePlayServicesRepairableException {
        when(adIdWrapper.getAdInfo()).thenReturn(Optional.of(new AdvertisingIdClient.Info(null, false)));

        adIdHelper.init();

        assertThat(adIdHelper.getAdId().isPresent()).isFalse();
    }

    @Test
    public void handlesMissingAdInfo() throws GooglePlayServicesNotAvailableException, IOException, GooglePlayServicesRepairableException {
        when(adIdWrapper.getAdInfo()).thenReturn(Optional.absent());

        adIdHelper.init();

        assertThat(adIdHelper.getAdId().isPresent()).isFalse();
        assertThat(adIdHelper.getAdIdTracking()).isFalse();
    }
}
