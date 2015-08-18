package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PolicyInfoTest {

    public static final String SCANDROIDAD1_MONETIZABLE_WITHOUT_LEAVEBEHIND = "soundcloud:tracks:172426518";

    @Test
    public void hasLegibleToString() {
        final PolicyInfo monetizable = new PolicyInfo(
                SCANDROIDAD1_MONETIZABLE_WITHOUT_LEAVEBEHIND, true, "monetizable", true);

        assertThat(monetizable.toString())
                .isEqualTo("PolicyInfo{trackUrn=soundcloud:tracks:172426518, monetizable=true, policy=monetizable, syncable=true}");
    }
}
