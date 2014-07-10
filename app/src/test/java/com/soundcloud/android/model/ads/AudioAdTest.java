package com.soundcloud.android.model.ads;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class AudioAdTest {

    @Test
    public void deserializesAudioAd() throws Exception {
        AudioAd audioAd = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("audio_ad.json"), AudioAd.class);

        expect(audioAd.getTrackSummary().getUrn()).toEqual(Urn.forTrack(96416915));
        expect(audioAd.getTrackingImpressionUrl()).toEqual("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A82%3Bview_key%3A1401441272728730%3Bzone_id%3A6&loc=&listenerId=38cfbf01fa3450d214e1cc37548d18&sessionId=7a9cb38dc0b93f35a2c1ee36de75c54b&ip=%3A%3Affff%3A62.72.64.50&user_agent=http.async.client%2F0.5.2&cbs=4354567");
        expect(audioAd.getTrackingPlayUrl()).toEqual("https://promoted.soundcloud.com/track?reqType=SCSoundPlayed&protocolVersion=2.0&adId=82&zoneId=6&cb=9fdb6c5778784758b2f064907b7b3485");
        expect(audioAd.getTrackingFinishUrl()).toEqual("https://promoted.soundcloud.com/track?reqType=SCSoundFinished&protocolVersion=2.0&adId=82&zoneId=6&cb=c34180f2e5d7433f8357879e355a58e0");
        expect(audioAd.getTrackingSkipUrl()).toEqual("https://promoted.soundcloud.com/track?reqType=SCSoundSkipped&protocolVersion=2.0&adId=82&zoneId=6&cb=882f90b95de847f9bfa8684943cf761f");

    }
}