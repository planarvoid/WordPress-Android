package com.soundcloud.android.api.model;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PolicyInfoTest {

    public static final String SCANDROIDAD1_MONETIZABLE_WITHOUT_LEAVEBEHIND = "soundcloud:tracks:172426518";

    @Test
    public void hasLegibleToString() {
        final PolicyInfo monetizable = new PolicyInfo(SCANDROIDAD1_MONETIZABLE_WITHOUT_LEAVEBEHIND, true, "monetizable", true);

        expect(monetizable.toString()).toEqual("PolicyInfo{trackUrn=soundcloud:tracks:172426518, monetizable=true, policy=monetizable, syncable=true}");
    }
}
