package com.soundcloud.gradle.violations;

import static se.bjurr.violations.comments.github.lib.ViolationCommentsToGitHubApi.violationCommentsToGitHubApi;
import static se.bjurr.violations.lib.ViolationsReporterApi.violationsReporterApi;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import se.bjurr.violations.comments.github.lib.GitHubCommentsProvider;
import se.bjurr.violations.comments.github.lib.ViolationCommentsToGitHubApi;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.CommentsProvider;
import se.bjurr.violations.lib.model.SEVERITY;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.reports.Reporter;
import se.bjurr.violations.lib.util.Filtering;
import se.bjurr.violations.lib.util.Optional;

import java.util.ArrayList;
import java.util.List;


/**
 * Based off of: https://github.com/tomasbjerre/violation-comments-to-github-gradle-plugin/blob/master/src/main/java/se/bjurr/violations/comments/github/plugin/gradle/ViolationCommentsToGitHubTask.java
 *
 * The original implementation only hands off the list of violations to the {@link ViolationCommentsToGitHubApi} class
 * which will then comment on the PR where needed.
 * This implementation extends this behavior so that this Task can also filter how many violations need to be reported
 * to the PR in GitHub. If there are any violations reported to GitHub, the task will throw an Exception and fail.
 */
@SuppressWarnings("ALL")
public class ViolationCommentsTask extends DefaultTask {

    private String repositoryOwner;
    private String repositoryName;

    private String pullRequestId;

    private String oAuth2Token;
    private String username;
    private String password;
    private String gitHubUrl;
    private List<List<String>> violations = new ArrayList<>();
    private boolean createCommentWithAllSingleFileComments = false;
    private boolean createSingleFileComments = true;
    private boolean commentOnlyChangedContent = true;
    private SEVERITY minSeverity;

    public void setCreateCommentWithAllSingleFileComments(
            boolean createCommentWithAllSingleFileComments) {
        this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
    }

    public void setCreateSingleFileComments(boolean createSingleFileComments) {
        this.createSingleFileComments = createSingleFileComments;
    }

    public void setCommentOnlyChangedContent(boolean commentOnlyChangedContent) {
        this.commentOnlyChangedContent = commentOnlyChangedContent;
    }

    public void setGitHubUrl(String gitHubUrl) {
        this.gitHubUrl = gitHubUrl;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public void setRepositoryOwner(String repositoryOwner) {
        this.repositoryOwner = repositoryOwner;
    }

    public void setPullRequestId(String pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    public void setViolations(List<List<String>> violations) {
        this.violations = violations;
    }

    public void setoAuth2Token(String oAuth2Token) {
        this.oAuth2Token = oAuth2Token;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMinSeverity(SEVERITY minSeverity) {
        this.minSeverity = minSeverity;
    }

    @TaskAction
    public void gitChangelogPluginTasks() throws TaskExecutionException {
        getLogger().info("Checking for Violations in the GitHub changelog");

        if (pullRequestId == null || pullRequestId.equalsIgnoreCase("false")) {
            getLogger().info("No pull request id defined, will not send violation comments to GitHub.");
            return;
        }
        Integer pullRequestIdInt = Integer.valueOf(pullRequestId);
        if (oAuth2Token != null) {
            getLogger().info("Using OAuth2Token");
        } else if (username != null && password != null) {
            getLogger().info("Using username/password: " + username.substring(0, 1) + ".../*********");
        } else {
            getLogger().info("No OAuth2 token and no username/email specified. Will not comment any pull request.");
            return;
        }

        getLogger().info("Will comment PR "
                                 + repositoryOwner
                                 + "/"
                                 + repositoryName
                                 + "/"
                                 + pullRequestId
                                 + " on "
                                 + gitHubUrl);


        List<Violation> allParsedViolations = new ArrayList<>();
        for (List<String> configuredViolation : violations) {
            List<Violation> parsedViolations =
                    violationsReporterApi() //
                                            .findAll(Reporter.valueOf(configuredViolation.get(0))) //
                                            .inFolder(configuredViolation.get(1)) //
                                            .withPattern(configuredViolation.get(2)) //
                                            .violations();
            if (minSeverity != null) {
                allParsedViolations = Filtering.withAtLEastSeverity(allParsedViolations, SEVERITY.INFO);
            }
            allParsedViolations.addAll(parsedViolations);
        }

        ViolationCommentsToGitHubApi violationCommentsToGitHubApi = violationCommentsToGitHubApi() //
                                                                                                   .withoAuth2Token(oAuth2Token) //
                                                                                                   .withUsername(username) //
                                                                                                   .withPassword(password) //
                                                                                                   .withPullRequestId(pullRequestIdInt) //
                                                                                                   .withRepositoryName(repositoryName) //
                                                                                                   .withRepositoryOwner(repositoryOwner) //
                                                                                                   .withGitHubUrl(gitHubUrl) //
                                                                                                   .withViolations(allParsedViolations) //
                                                                                                   .withCreateCommentWithAllSingleFileComments(createCommentWithAllSingleFileComments) //
                                                                                                   .withCreateSingleFileComments(createSingleFileComments) //
                                                                                                   .withCommentOnlyChangedContent(commentOnlyChangedContent); //

        try {
            violationCommentsToGitHubApi.toPullRequest();
        } catch (Exception e) {
            getLogger().error("", e);
        }


        CommentsProvider commentsProvider = new GitHubCommentsProvider(violationCommentsToGitHubApi);
        List<ChangedFile> changedFiles = commentsProvider.getFiles();
        List<Violation> filteredViolations = filterChanged(allParsedViolations, changedFiles, commentsProvider);
        List<Violation> errors = getAllErrors(filteredViolations);

        if (!errors.isEmpty()) {
            String message = String.format(
                    "Found %d violations, failing the static analysis stage. Fix these violations that were also reported as comments on the GitHub Pull Request to make this stage succeed.",
                    filteredViolations.size());
            throw new TaskExecutionException(this, new StaticAnalysisFoundViolationException(message));
        }

    }

    private List<Violation> getAllErrors(List<Violation> filteredViolations) {
        List<Violation> errors = new ArrayList<>();
        for (Violation violation : filteredViolations) {
            if (violation.getSeverity() == SEVERITY.ERROR) {
                errors.add(violation);
            }
        }
        return errors;
    }

    private List<Violation> filterChanged(List<Violation> allViolations, List<ChangedFile> changedFiles, CommentsProvider commentsProvider) {
        List<Violation> isChanged = new ArrayList<>();
        for (Violation violation : allViolations) {
            Optional<ChangedFile> file = getFile(violation, changedFiles);
            if (file.isPresent()) {
                if (commentsProvider.shouldComment(file.get(), violation.getStartLine())) {
                    isChanged.add(violation);
                }
            }
        }
        return isChanged;
    }

    private Optional<ChangedFile> getFile(Violation violation, List<ChangedFile> changedFiles) {
        for (ChangedFile providerFile : changedFiles) {
            if (violation.getFile().endsWith(providerFile.getFilename()) || providerFile.getFilename().endsWith(violation.getFile())) {
                return Optional.fromNullable(providerFile);
            }
        }
        return Optional.absent();
    }
}
