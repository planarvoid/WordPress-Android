package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiAudioAdTest {

    @Test
    public void deserializesAudioAd() throws Exception {
        ApiAudioAd audioAd = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("audio_ad.json"), ApiAudioAd.class);

        expect(audioAd.getApiTrack().getUrn()).toEqual(Urn.forTrack(96416915));
        expect(audioAd.trackingImpressionUrls).toEqual(newArrayList("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A82%3Bview_key%3A1401441272728730%3Bzone_id%3A6&loc=&listenerId=38cfbf01fa3450d214e1cc37548d18&sessionId=7a9cb38dc0b93f35a2c1ee36de75c54b&ip=%3A%3Affff%3A62.72.64.50&user_agent=http.async.client%2F0.5.2&cbs=4354567"));
        expect(audioAd.trackingFinishUrls).toEqual(newArrayList("https://promoted.soundcloud.com/track?reqType=SCSoundFinished&protocolVersion=2.0&adId=82&zoneId=6&cb=c34180f2e5d7433f8357879e355a58e0"));
        expect(audioAd.trackingSkipUrls).toEqual(newArrayList("https://promoted.soundcloud.com/track?reqType=SCSoundSkipped&protocolVersion=2.0&adId=82&zoneId=6&cb=882f90b95de847f9bfa8684943cf761f"));
        expect(audioAd.getLeaveBehind().imageUrl).toEqual("https://va.sndcdn.com/mlb/sqsp-example-leave-behind.jpg");
        expect(audioAd.getLeaveBehind().clickthroughUrl).toEqual("http://squarespace.com");
        expect(audioAd.getLeaveBehind().trackingImpressionUrls).toContain("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A1105%3Bview_key%3A1410853892331806%3Bzone_id%3A56&loc=&listenerId=5284047f4ffb4e04824a2fd1d1f0cd62&sessionId=67fa476869b956676b5bae2866c377a9&ip=%3A%3Affff%3A80.82.202.196&OAGEO=ZGUlN0MxNiU3Q2JlcmxpbiU3QzEwMTE1JTdDNTIuNTMxOTk3NjgwNjY0MDYlN0MxMy4zOTIxOTY2NTUyNzM0MzglN0MlN0MlN0MlN0MlM0ElM0FmZmZmJTNBODAuODIuMjAyLjE5NiU3Q3RoZSt1bmJlbGlldmFibGUrbWFjaGluZStjb21wYW55K2dtYmg=&user_agent=SoundCloud-Android%2F14.09.02+%28Android+4.3%3B+Genymotion+Sony+Xperia+Z+-+4.3+-+API+18+-+1080x1920%29&cbs=681405");
        expect(audioAd.getLeaveBehind().trackingClickUrls).toContain("https://promoted.soundcloud.com/track?reqType=SCAdClicked&protocolVersion=2.0&adId=1105&zoneId=56&cb=dfd1b6e0c90745e9934f9d35b174ff30");
    }

    @Test
    public void extractsIndividualUrnsForAdComponents() throws Exception {
        ApiAudioAd audioAd = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("audio_ad.json"), ApiAudioAd.class);

        final PropertySet audioAdProperties = audioAd.toPropertySet();
        final PropertySet leaveBehindProperties = audioAd.getLeaveBehind().toPropertySet();

        expect(audioAdProperties.get(AdProperty.AUDIO_AD_URN)).toEqual("adswizz:ads:263");
        expect(audioAdProperties.get(AdProperty.COMPANION_URN)).toEqual("adswizz:ads:954");
        expect(leaveBehindProperties.get(LeaveBehindProperty.LEAVE_BEHIND_URN)).toEqual("adswizz:ads:1105");
    }

    @Test
    public void resolvesToPropertySet() throws CreateModelException {
        ApiAudioAd audioAd = ModelFixtures.create(ApiAudioAd.class);

        final PropertySet propertySet = audioAd.toPropertySet();

        expect(propertySet.contains(AdProperty.AUDIO_AD_URN)).toBeTrue();
        expect(propertySet.contains(AdProperty.COMPANION_URN)).toBeTrue();
        expect(propertySet.contains(AdProperty.ARTWORK)).toBeTrue();
        expect(propertySet.contains(AdProperty.CLICK_THROUGH_LINK)).toBeTrue();
        expect(propertySet.contains(AdProperty.DEFAULT_TEXT_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.DEFAULT_BACKGROUND_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.PRESSED_TEXT_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.PRESSED_BACKGROUND_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.FOCUSED_TEXT_COLOR)).toBeTrue();
        expect(propertySet.contains(AdProperty.FOCUSED_BACKGROUND_COLOR)).toBeTrue();

        // tracking urls for promoted
        expect(propertySet.contains(AdProperty.AUDIO_AD_IMPRESSION_URLS)).toBeTrue();
        expect(propertySet.contains(AdProperty.AUDIO_AD_FINISH_URLS)).toBeTrue();
        expect(propertySet.contains(AdProperty.AUDIO_AD_CLICKTHROUGH_URLS)).toBeTrue();
        expect(propertySet.contains(AdProperty.AUDIO_AD_SKIP_URLS)).toBeTrue();
        expect(propertySet.contains(AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS)).toBeTrue();
    }
}
