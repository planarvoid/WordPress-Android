package com.soundcloud.android.lint.rules;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.soundcloud.android.lint.rules.checks.EnumDetector;
import com.soundcloud.android.lint.rules.checks.IntentFactoryDetector;
import com.soundcloud.android.lint.rules.checks.LogDetector;
import com.soundcloud.android.lint.rules.checks.NavigatorDetector;
import com.soundcloud.android.lint.rules.checks.RxJava1Detector;
import com.soundcloud.android.lint.rules.checks.RxJava2Detector;

import java.util.Arrays;
import java.util.List;

public class SoundCloudIssueRegistry extends IssueRegistry {
    @Override
    public List<Issue> getIssues() {
        return Arrays.asList(
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
