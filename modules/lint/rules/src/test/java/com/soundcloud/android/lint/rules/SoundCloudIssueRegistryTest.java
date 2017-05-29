package com.soundcloud.android.lint.rules;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

public class SoundCloudIssueRegistryTest {

    private SoundCloudIssueRegistry issueRegistry;

    @Before
    public void setUp() throws Exception {
        issueRegistry = new SoundCloudIssueRegistry();
    }

    @Test
    public void assertTests() throws Exception {
        assertThat(issueRegistry.getIssues()).isEmpty();
    }
}
