package com.soundcloud.android.lint.rules;

import static com.google.common.truth.Truth.assertThat;

import com.soundcloud.android.lint.rules.checks.IntentFactoryDetector;
import com.soundcloud.android.lint.rules.checks.NavigatorDetector;
import org.junit.Before;
import org.junit.Test;

public class SoundCloudIssueRegistryTest {

    private SoundCloudIssueRegistry issueRegistry;

    @Before
    public void setUp() throws Exception {
        issueRegistry = new SoundCloudIssueRegistry();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void assertTests() throws Exception {
        assertThat(issueRegistry.getIssues()).containsExactly(NavigatorDetector.ISSUE_START_INTENT, IntentFactoryDetector.ISSUE_CREATE_OUTSIDE);
    }
}
