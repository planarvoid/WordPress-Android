package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AdFunctionsTest extends AndroidUnitTest {

    @Test
    public void hasAdUrnShouldReturnTrueForAudioAdPropertySet() {
        final PropertySet adMetaData = TestPropertySets.audioAdProperties(Urn.forTrack(123L));
        assertThat(AdFunctions.HAS_AD_URN.apply(adMetaData)).isTrue();
    }

    @Test
    public void hasAdUrnShouldReturnFalseForInterstitialPropertySet() {
        final PropertySet adMetaData = TestPropertySets.interstitialForPlayer();
        assertThat(AdFunctions.HAS_AD_URN.apply(adMetaData)).isFalse();
    }

    @Test
    public void hasAdUrnShouldReturnFalseForEmptyPropertySet() {
        assertThat(AdFunctions.HAS_AD_URN.apply(PropertySet.create())).isFalse();
    }

}