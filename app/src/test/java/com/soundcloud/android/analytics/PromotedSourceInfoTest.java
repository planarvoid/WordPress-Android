package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import org.junit.Test;

import android.os.Parcel;

public class PromotedSourceInfoTest extends AndroidUnitTest {

    @Test
    public void implementsParcelable() {
        PromotedListItem promotedListItem = PlayableFixtures.expectedPromotedTrack();
        Parcel parcel = Parcel.obtain();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedListItem);
        promotedSourceInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PromotedSourceInfo copy = new PromotedSourceInfo(parcel);
        assertThat(copy.getAdUrn()).isEqualTo(promotedSourceInfo.getAdUrn());
        assertThat(copy.getPromotedItemUrn()).isEqualTo(promotedSourceInfo.getPromotedItemUrn());
        assertThat(copy.getPromoterUrn()).isEqualTo(promotedSourceInfo.getPromoterUrn());
        assertThat(copy.getTrackingUrls()).isEqualTo(promotedSourceInfo.getTrackingUrls());
    }

    @Test
    public void parcelsOptionalPromoterUrn() {
        PromotedListItem promotedListItem = PlayableFixtures.expectedPromotedTrackWithoutPromoter();
        Parcel parcel = Parcel.obtain();
        PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedListItem);
        promotedSourceInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PromotedSourceInfo copy = new PromotedSourceInfo(parcel);
        assertThat(copy.getPromoterUrn()).isEqualTo(promotedSourceInfo.getPromoterUrn());
        assertThat(copy.getPromoterUrn().isPresent()).isFalse();
    }
}
