package com.soundcloud.android.analytics;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.tracks.PromotedTrackItem;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

@RunWith(SoundCloudTestRunner.class)
public class PromotedSourceInfoTest {

    @Test
    public void implementsParcelable() {
        PromotedListItem promotedListItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        Parcel parcel = Parcel.obtain();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedListItem);
        promotedSourceInfo.writeToParcel(parcel, 0);

        PromotedSourceInfo copy = new PromotedSourceInfo(parcel);
        expect(copy.getAdUrn()).toEqual(promotedSourceInfo.getAdUrn());
        expect(copy.getPromotedItemUrn()).toEqual(promotedSourceInfo.getPromotedItemUrn());
        expect(copy.getPromoterUrn()).toEqual(promotedSourceInfo.getPromoterUrn());
        expect(copy.getTrackingUrls()).toEqual(promotedSourceInfo.getTrackingUrls());
    }

    @Test
    public void parcelsOptionalPromoterUrn() {
        PromotedListItem promotedListItem = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrackWithoutPromoter());
        Parcel parcel = Parcel.obtain();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedListItem);
        promotedSourceInfo.writeToParcel(parcel, 0);

        PromotedSourceInfo copy = new PromotedSourceInfo(parcel);
        expect(copy.getPromoterUrn()).toEqual(promotedSourceInfo.getPromoterUrn());
        expect(copy.getPromoterUrn().isPresent()).toBeFalse();
    }

}
