package com.soundcloud.android.analytics;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PromotedSourceInfoTest {

    private PromotedSourceInfo promotedSourceInfo;

    @Before
    public void setUp() throws Exception {
        PromotedTrackItem promotedTrackItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        promotedSourceInfo = PromotedSourceInfo.fromTrack(promotedTrackItem);
    }

    @Test
    public void implementsParcelable() {
        Parcel parcel = Parcel.obtain();
        promotedSourceInfo.writeToParcel(parcel, 0);

        PromotedSourceInfo copy = new PromotedSourceInfo(parcel);
        expect(copy.getAdUrn()).toEqual(promotedSourceInfo.getAdUrn());
        expect(copy.getTrackUrn()).toEqual(promotedSourceInfo.getTrackUrn());
        expect(copy.getPromoterUrn()).toEqual(promotedSourceInfo.getPromoterUrn());
        expect(copy.getTrackingUrls()).toEqual(promotedSourceInfo.getTrackingUrls());
    }

}
