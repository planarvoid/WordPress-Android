package com.soundcloud.android.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalConfigTest {

    private static final Flag FLAG = Flag.TEST_FEATURE;

    private LocalConfig localConfig;

    @Before
    public void setUp() {
        localConfig = new LocalConfig();
    }

    @Test
    public void shouldReturnCompileTimeValue() {
        assertThat(localConfig.getFlagValue(FLAG)).isEqualTo(FLAG.featureValue());
    }
}
