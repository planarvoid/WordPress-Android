package com.soundcloud.android.lint.rules;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.soundcloud.android.lint.rules.checks.IntentFactoryDetector;
import com.soundcloud.android.lint.rules.checks.NavigatorDetector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SoundCloudIssueRegistry extends IssueRegistry {
    @Override
    public List<Issue> getIssues() {
        return Arrays.asList(NavigatorDetector.ISSUE_START_INTENT, IntentFactoryDetector.ISSUE_CREATE_OUTSIDE);
    }
}
