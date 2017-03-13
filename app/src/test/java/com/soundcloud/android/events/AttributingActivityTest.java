package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AttributingActivityTest {

    @Test
    public void isActiveWhenModuleIsStream() {
        final AttributingActivity attributingActivity = AttributingActivity.create("what", Optional.absent());
        final Module module = Module.create(Module.STREAM, 1);

        assertThat(attributingActivity.isActive(Optional.of(module))).isTrue();
    }

    @Test
    public void isNotActiveWhenModuleIsNotStream() {
        final AttributingActivity attributingActivity = AttributingActivity.create("what", Optional.absent());
        final Module module = Module.create("word", 1);

        assertThat(attributingActivity.isActive(Optional.of(module))).isFalse();
    }
}
