package com.soundcloud.android.lint.rules;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

public class SoundCloudIssueRegistry extends IssueRegistry {
    @Override
    public List<Issue> getIssues() {
        return Collections.emptyList();
    }
}
