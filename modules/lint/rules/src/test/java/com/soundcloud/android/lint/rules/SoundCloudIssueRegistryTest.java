package com.soundcloud.android.lint.rules;

import static com.google.common.truth.Truth.assertThat;

import com.soundcloud.android.lint.rules.checks.EnumDetector;
import com.soundcloud.android.lint.rules.checks.IntentFactoryDetector;
import com.soundcloud.android.lint.rules.checks.LogDetector;
import com.soundcloud.android.lint.rules.checks.NavigatorDetector;
import com.soundcloud.android.lint.rules.checks.RxJava1Detector;
import com.soundcloud.android.lint.rules.checks.RxJava2Detector;
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
        assertThat(issueRegistry.getIssues()).containsExactly(
                NavigatorDetector.ISSUE_START_INTENT,
                IntentFactoryDetector.ISSUE_CREATE_OUTSIDE,
                EnumDetector.ISSUE_ENUM_USAGE,
                RxJava2Detector.ISSUE_METHOD_MISSING_CHECK_RESULT,
                RxJava2Detector.ISSUE_MISSING_COMPOSITE_DISPOSABLE_RECYCLED,
                RxJava2Detector.ISSUE_DISPOSE_COMPOSITE_DISPOSABLE,
                LogDetector.ISSUE_ANDROID_LOG,
                RxJava1Detector.ISSUE_RXJAVA_1_USAGE
        );
    }
}
