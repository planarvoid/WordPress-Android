package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiCompanionAdTest {

    @Test
    public void deserializesVisualAdWithButton() throws Exception {
        ApiCompanionAd companionVisual = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("visual_ad_with_button.json"), ApiCompanionAd.class);

        expect(companionVisual.urn).toEqual("adswizz:ads:82");
        expect(companionVisual.imageUrl).toEqual("https://va.sndcdn.com/da/ad_user-likes_v3.jpg");
        expect(companionVisual.clickthroughUrl).toEqual("https://www.soundcloud.com/you/likes");
        expect(companionVisual.trackingClickUrls).toEqual(newArrayList("https://promoted.soundcloud.com/track?reqType=SCAdClicked&protocolVersion=2.0&adId=263&zoneId=19&cb=7a76b73c420c4cb8b171f083b1f59a05"));
        expect(companionVisual.trackingImpressionUrls).toEqual(newArrayList("https://promoted.soundcloud.com/impression?adData=instance%3Asoundcloud%3Bad_id%3A263%3Bview_key%3A1401441272790582%3Bzone_id%3A19&loc=&listenerId=38cfbf01fa3450d214e1cc37548d18&sessionId=3e89b0ad42cff51ed4de13fd8ec97497&ip=%3A%3Affff%3A62.72.64.50&user_agent=http.async.client%2F0.5.2&cbs=5346259"));

        expect(companionVisual.displayProperties).not.toBeNull();
    }
}