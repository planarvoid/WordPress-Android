package com.soundcloud.android.api.legacy.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import android.os.Parcel;

public class PublicApiCommentTest extends AndroidUnitTest {
    @Test
    public void parcelizes() {
        PublicApiComment before = ModelFixtures.publicApiComment();
        Parcel parcel = Parcel.obtain();
        before.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PublicApiComment after = PublicApiComment.CREATOR.createFromParcel(parcel);

        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getUrn()).isEqualTo(before.getUrn());
        assertThat(after.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(after.getUser()).isEqualTo(before.getUser());
        assertThat(after.getBody()).isEqualTo(before.getBody());
        assertThat(after.getTrack()).isEqualTo(before.getTrack());
    }
}
