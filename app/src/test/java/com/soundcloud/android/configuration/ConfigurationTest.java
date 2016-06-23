package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.configuration.features.Feature;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ConfigurationTest {

    @Test
    public void emptyAssignmentWhenExperimentsIsMissing() {
        Configuration configuration = Configuration.create(Arrays.asList(
                new Feature("feature", false, Arrays.asList("mid_tier"))),
                                                           new UserPlan("free", Collections.<String>emptyList()), null,
                                                           new DeviceManagement(true, false),
                                                           false, Collections.<String>emptyList());
        assertThat(configuration.getAssignment().isEmpty()).isTrue();
    }

}
