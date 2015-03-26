package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class AdIdHelperTest {

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

        expect(adIdHelper.isAvailable()).toBeTrue();
        expect(adIdHelper.getAdId()).toEqual("my-adid");
    }

    @Test
    public void adIdIsNotLoadedIfPlayServicesIsUnavailable() {
        when(adIdWrapper.isPlayServicesAvailable()).thenReturn(false);

        adIdHelper.init();

        expect(adIdHelper.isAvailable()).toBeFalse();
    }

    @Test
    public void googleTrackingBooleanIsInvertedToMatchAppleEquivalent() {
        adIdHelper.init();

        expect(adIdHelper.isAvailable()).toBeTrue();
        expect(adIdHelper.getAdIdTracking()).toBeTrue();
    }

    @Test
    public void adIdIsNotAvailableUntilLoaded() {
        expect(adIdHelper.isAvailable()).toBeFalse();
    }

    @Test
    public void adIdIsNotAvailableOnError() throws Exception {
        when(adIdWrapper.getAdInfo()).thenThrow(new GooglePlayServicesNotAvailableException(123));

        adIdHelper.init();

        expect(adIdHelper.isAvailable()).toBeFalse();
    }
    
}
