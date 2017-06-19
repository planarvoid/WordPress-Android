package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

public class LogDetectorTest extends LintDetectorTest {

    protected Detector getDetector() {
        return new LogDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(LogDetector.ISSUE_ANDROID_LOG);
    }

    public void test_androidlog_reports() throws Exception {
        lint().files(java("package foo;\n" +
                                  "\n" +
                                  "import android.util.Log;\n" +
                                  "\n" +
                                  "public class Example {\n" +
                                  "  public void start() {\n" +
                                  "    Log.d(\"TEST\", \"Log something\");\n" +
                                  "  }\n" +
                                  "}"))
              .run()
              .expect("src/foo/Example.java:7: Warning: Android Log usage detected. [sc.AndroidLog]\n" +
                              "    Log.d(\"TEST\", \"Log something\");\n" +
                              "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void test_soundcloudLog_clean() throws Exception {
        lint().files(soundcloudLogClass(), java("package foo;\n" +
                                                        "\n" +
                                                        "import com.soundcloud.android.utils.Log;\n" +
                                                        "\n" +
                                                        "public class Example {\n" +
                                                        "  public void start() {\n" +
                                                        "    Log.d(\"TEST\", \"Log something\");\n" +
                                                        "  }\n" +
                                                        "}"))
              .run()
              .expectClean();
    }


    public void test_random_d_method() throws Exception {
        lint().files(soundcloudLogClass(), java("package foo;\n" +
                                                        "\n" +
                                                        "import com.soundcloud.android.utils.Log;\n" +
                                                        "\n" +
                                                        "public class Example {\n" +
                                                        "  public void start() {\n" +
                                                        "    d(\"TEST\", \"Log something\");\n" +
                                                        "  }\n" +
                                                        "  private void d(String tag, String message) {\n" +
                                                        "  }\n" +
                                                        "}"))
              .run()
              .expectClean();
    }

    public void test_soundcloudLog_canUseAndroidLog() throws Exception {
        lint().files(soundcloudLogClass())
              .run()
              .expectClean();
    }


    private static TestFile soundcloudLogClass() {
        return java("package com.soundcloud.android.utils;\n" +
                            "\n" +
                            "public class Log {\n" +
                            "  public static void d(String tag, String message) {\n" +
                            "    android.util.Log.d(\"TEST\", \"Log something\");\n" +
                            "  }\n" +
                            "}");
    }
}
