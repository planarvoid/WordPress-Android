package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.configuration.features.Feature;
import org.junit.Test;

import java.util.Arrays;

public class ConfigurationTest {

    @Test
    public void emptyAssignmentWhenExperimentsIsMissing() {
        Configuration configuration = new Configuration(Arrays.asList(
                new Feature("feature", false, Arrays.asList("mid_tier"))),
                new UserPlan("free", null), null,
                new DeviceManagement(true, null));
        assertThat(configuration.assignment.isEmpty()).isTrue();
    }

}
