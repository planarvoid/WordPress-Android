package com.soundcloud.android.configuration;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.configuration.features.Feature;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ConfigurationTest {

    @Test
    public void emptyAssignmentWhenExperimentsIsMissing() {
        Configuration configuration = new Configuration(Arrays.asList(
                new Feature("feature", false, Arrays.asList("mid_tier"))),
                new UserPlan("free", null), null,
                new DeviceManagement(true, null));
        expect(configuration.assignment.isEmpty()).toBeTrue();
    }

}
