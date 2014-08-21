package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class AudioAdTest {

    @Test
    public void deserializesAudioAd() throws Exception {
        AudioAd audioAd = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("audio_ad.json"), AudioAd.class);

        expect(audioAd.getApiTrack().getUrn()).toEqual(Urn.forTrack(96416915));
        expect(audioAd.getTrackingImpressionUrls()).toEqual(Lists.newArrayList("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A82%3Bview_key%3A1401441272728730%3Bzone_id%3A6&loc=&listenerId=38cfbf01fa3450d214e1cc37548d18&sessionId=7a9cb38dc0b93f35a2c1ee36de75c54b&ip=%3A%3Affff%3A62.72.64.50&user_agent=http.async.client%2F0.5.2&cbs=4354567"));
        expect(audioAd.getTrackingFinishUrls()).toEqual(Lists.newArrayList("https://promoted.soundcloud.com/track?reqType=SCSoundFinished&protocolVersion=2.0&adId=82&zoneId=6&cb=c34180f2e5d7433f8357879e355a58e0"));
        expect(audioAd.getTrackingSkipUrls()).toEqual(Lists.newArrayList("https://promoted.soundcloud.com/track?reqType=SCSoundSkipped&protocolVersion=2.0&adId=82&zoneId=6&cb=882f90b95de847f9bfa8684943cf761f"));

    }

    @Test
    public void shouldResolveToPropertySet() throws CreateModelException {
        AudioAd audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
        final PropertySet propertySet = audioAd.toPropertySet();
        expect(propertySet.contains(AdProperty.AD_URN)).toBeTrue();
        expect(propertySet.contains(AdProperty.ARTWORK)).toBeTrue();
        expect(propertySet.contains(AdProperty.CLICK_THROUGH_LINK)).toBeTrue();
        expect(propertySet.contains(AdProperty.DEFAULT_TEXT_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.DEFAULT_BACKGROUND_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.PRESSED_TEXT_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.PRESSED_BACKGROUND_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.FOCUSED_TEXT_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.FOCUSED_BACKGROUND_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS)).toBeTrue();
    }
}