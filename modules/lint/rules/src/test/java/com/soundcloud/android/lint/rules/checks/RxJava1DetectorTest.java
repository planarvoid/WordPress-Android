package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

public class RxJava1DetectorTest extends LintDetectorTest {

    @Override
    protected Detector getDetector() {
        return new RxJava1Detector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(RxJava1Detector.ISSUE_RXJAVA_1_USAGE);
    }

    public void test_RxJava1_method_using_rxjava() throws Exception {
        lint().files(observable(), java("package foo;\n" +
                                                "\n" +
                                                "import rx.Observable;\n" +
                                                "\n" +
                                                "public class Example {\n" +
                                                "  public void start() {\n" +
                                                "    " + "rx.Observable observable = rx.Observable.just(1);\n" +
                                                "  }\n" +
                                                "}"))
              .run()
              .expect("src/foo/Example.java:3: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "import rx.Observable;\n" +
                              "~~~~~~~~~~~~~~~~~~~~~\n" +
                              "src/foo/Example.java:7: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "    rx.Observable observable = rx.Observable.just(1);\n" +
                              "    ~~~~~~~~~~~~~\n" +
                              "src/foo/Example.java:7: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "    rx.Observable observable = rx.Observable.just(1);\n" +
                              "                                             ~~~~\n" +
                              "src/rx/Observable.java:4: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  public static Observable just(int number) {\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 4 warnings\n");
    }

    public void test_RxJava1_import() throws Exception {
        lint().files(observable(), java("package foo;\n" +
                                                "\n" +
                                                "import rx.Observable;\n" +
                                                "public class Example {\n" +
                                                "}"))
              .run()
              .expect("src/foo/Example.java:3: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "import rx.Observable;\n" +
                              "~~~~~~~~~~~~~~~~~~~~~\n" +
                              "src/rx/Observable.java:4: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  public static Observable just(int number) {\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 2 warnings\n");
    }

    public void test_RxJava1_field() throws Exception {
        lint().files(observable(), java("package foo;\n" +
                                                "\n" +
                                                "import rx.Observable;\n" +
                                                "\n" +
                                                "public class Example {\n" +
                                                "  private rx.Observable observable;\n" +
                                                "}"))
              .run()
              .expect("src/foo/Example.java:3: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "import rx.Observable;\n" +
                              "~~~~~~~~~~~~~~~~~~~~~\n" +
                              "src/foo/Example.java:6: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  private rx.Observable observable;\n" +
                              "          ~~~~~~~~~~~~~\n" +
                              "src/rx/Observable.java:4: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  public static Observable just(int number) {\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 3 warnings\n");
    }

    public void test_RxJava1_chained_usage() throws Exception {
        lint().files(observable(), java("package foo;\n" +
                                                "\n" +
                                                "import rx.Observable;\n" +
                                                "\n" +
                                                "public class Example {\n" +
                                                "  public void start() {\n" +
                                                "    " + "rx.Observable.just(1).just(1);\n" +
                                                "  }\n" +
                                                "}"))
              .run()
              .expect("src/foo/Example.java:3: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "import rx.Observable;\n" +
                              "~~~~~~~~~~~~~~~~~~~~~\n" +
                              "src/foo/Example.java:7: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "    rx.Observable.just(1).just(1);\n" +
                              "                  ~~~~\n" +
                              "src/foo/Example.java:7: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "    rx.Observable.just(1).just(1);\n" +
                              "                          ~~~~\n" +
                              "src/rx/Observable.java:4: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  public static Observable just(int number) {\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 4 warnings\n");
    }

    public void test_RxJava1_method_return_type() throws Exception {
        lint().files(observable(), java("package foo;\n" +
                                                "\n" +
                                                "import rx.Observable;\n" +
                                                "\n" +
                                                "public class Example {\n" +
                                                "  public rx.Observable start() {\n" +
                                                "    " + "return rx.Observable.just(1);\n" +
                                                "  }\n" +
                                                "}"))
              .run()
              .expect("src/foo/Example.java:3: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "import rx.Observable;\n" +
                              "~~~~~~~~~~~~~~~~~~~~~\n" +
                              "src/foo/Example.java:6: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  public rx.Observable start() {\n" +
                              "         ~~~~~~~~~~~~~\n" +
                              "src/foo/Example.java:7: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "    return rx.Observable.just(1);\n" +
                              "                         ~~~~\n" +
                              "src/rx/Observable.java:4: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  public static Observable just(int number) {\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 4 warnings\n");
    }

    public void test_RxJava1_subclass() throws Exception {
        lint().files(observable(), java("package foo;\n" +
                                                "\n" +
                                                "import rx.Observable;\n" +
                                                "\n" +
                                                "public class MyObservable extends rx.Observable {\n" +
                                                "}"))
              .run()
              .expect("src/foo/MyObservable.java:3: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "import rx.Observable;\n" +
                              "~~~~~~~~~~~~~~~~~~~~~\n" +
                              "src/foo/MyObservable.java:5: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "public class MyObservable extends rx.Observable {\n" +
                              "^\n" +
                              "src/rx/Observable.java:4: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "  public static Observable just(int number) {\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 3 warnings\n");
    }

    public void test_RxJava1_subclass_generic() throws Exception {
        lint().files(genericSingle(), java("foo/MySingle.java",
                                           "package foo;\n" +
                                                   "\n" +
                                                   "import rx.Single;\n" +
                                                   "\n" +
                                                   "public class MyObservable extends rx.Single<String> {\n" +
                                                   "}"))
              .run()
              .expect("src/rx/Single.java:4: Warning: Migrate to RxJava2. RxJava1 should no longer be used. [sc.RxJava1Usage]\n" +
                              "    public static <T> Single<T> just(T item) {\n" +
                              "                      ~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    // Mock RxJava1 classes
    private static TestFile observable() {
        return java("package rx;\n" +
                            "\n" +
                            "public class Observable {\n" +
                            "  public static Observable just(int number) {\n" +
                            "    " + "return new Observable();\n" +
                            "  }\n" +
                            "}");
    }

    private static LintDetectorTest.TestFile genericSingle() {
        return java("rx/Single.java",
                    "package rx;\n" +
                            "\n" +
                            "public class Single<T> {\n" +
                            "    public static <T> Single<T> just(T item) {\n" +
                            "        return new Single<>();\n" +
                            "    }\n" +
                            "}").within("src");
    }

}
