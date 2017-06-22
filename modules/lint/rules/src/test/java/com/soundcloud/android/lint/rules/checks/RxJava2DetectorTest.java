package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.checks.infrastructure.ProjectDescription;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.List;

public class RxJava2DetectorTest extends BaseRxJava2DetectorTest {

    @Override
    protected Detector getDetector() {
        return new RxJava2Detector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Arrays.asList(RxJava2Detector.ISSUE_METHOD_MISSING_CHECK_RESULT,
                             RxJava2Detector.ISSUE_MISSING_COMPOSITE_DISPOSABLE_RECYCLED,
                             RxJava2Detector.ISSUE_DISPOSE_COMPOSITE_DISPOSABLE);
    }

    // ISSUE_METHOD_MISSING_CHECK_RESULT

    public void testMethodReturningNonRxType() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "public class Example {\n" +
                "  public void helloWorld() {\n" +
                "      test();\n" +
                "  }\n" +
                "  \n" +
                "  public String test() {\n" +
                "      return \"abc\";\n" +
                "  }\n" +
                "}";

        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningObservableMissingUsageCallingSuppressed() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Observable;\n"
                + "import java.lang.SuppressWarnings;\n"
                + "public class Example {\n"
                + "  @SuppressWarnings(\"sc.CheckResult\") "
                + "  public void helloWorld() {\n"
                + "     RxProvider.single();\n"
                + "  }\n"
                + "}";

        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningObservableMissingUsageReturningSuppressed() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Observable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     SuppressedRxProvider.single();\n"
                + "  }\n"
                + "}";

        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningObservableAndForgetGetsSuppressed() throws Exception {
        @Language("JAVA") final String source = "package foo;\n" +
                "import io.reactivex.Single;\n" +
                "public class Example {\n" +
                "  public void helloWorld() {\n" +
                "      singleAndForget();\n" +
                "  }\n" +
                "  \n" +
                "  public Single<String> singleAndForget() {\n" +
                "      return null;\n" +
                "  }\n" +
                "}";

        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningObservableMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Observable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.observable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of observable is not used. [sc.CheckResult]\n" +
                              "     RxProvider.observable();\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningObservableHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Observable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     Observable<String> var = RxProvider.observable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningFlowableMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Flowable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.flowable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of flowable is not used. [sc.CheckResult]\n" +
                              "     RxProvider.flowable();\n" +
                              "                ~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningFlowableHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Flowable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     Flowable<String> var = RxProvider.flowable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningSingleMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Single;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.single();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of single is not used. [sc.CheckResult]\n" +
                              "     RxProvider.single();\n" +
                              "                ~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningSingleHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Single;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     Single<String> var = RxProvider.single();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningMaybeMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Maybe;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.maybe();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of maybe is not used. [sc.CheckResult]\n" +
                              "     RxProvider.maybe();\n" +
                              "                ~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningMaybeHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Maybe;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     Maybe<String> var = RxProvider.maybe();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningCompletableMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Completable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.completable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of completable is not used. [sc.CheckResult]\n" +
                              "     RxProvider.completable();\n" +
                              "                ~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningCompletableHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Completable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     Completable var = RxProvider.completable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningDisposableMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.Disposable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.disposable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of disposable is not used. [sc.CheckResult]\n" +
                              "     RxProvider.disposable();\n" +
                              "                ~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningDisposableHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.Disposable;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     Disposable var = RxProvider.disposable();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningTestObserverMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.observers.TestObserver;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.testObserver();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of testObserver is not used. [sc.CheckResult]\n" +
                              "     RxProvider.testObserver();\n" +
                              "                ~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningTestObserverHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.observers.TestObserver;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     TestObserver<String> var = RxProvider.testObserver();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningTestSubscriberMissing() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.subscribers.TestSubscriber;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     RxProvider.testSubscriber();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expect("src/foo/Example.java:5: Warning: The result of testSubscriber is not used. [sc.CheckResult]\n" +
                              "     RxProvider.testSubscriber();\n" +
                              "                ~~~~~~~~~~~~~~\n" +
                              "0 errors, 1 warnings\n");
    }

    public void testMethodReturningTestSubscriberHaving() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.subscribers.TestSubscriber;\n"
                + "public class Example {\n"
                + "  public void helloWorld() {\n"
                + "     TestSubscriber<String> var = RxProvider.testSubscriber();\n"
                + "  }\n"
                + "}";
        lint().projects(getProject(source))
              .run()
              .expectClean();
    }

    // ISSUE_MISSING_COMPOSITE_DISPOSABLE_RECYCLED

    public void testNoCompositeDisposable() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "public class Example {\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expectClean();
    }

    public void testCompositeDisposableMissingClearSuppressed() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "import java.lang.SuppressWarnings;\n"
                + "public class Example {\n"
                + "  @SuppressWarnings(\"sc.MissingCompositeDisposableRecycle\") CompositeDisposable cd;\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expectClean();
    }

    public void testCompositeDisposableMissingClear() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "public class Example {\n"
                + "  CompositeDisposable cd;\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expect("src/foo/Example.java:4: Error: cd is never recycled. [sc.MissingCompositeDisposableRecycle]\n"
                              + "  CompositeDisposable cd;\n"
                              + "  ~~~~~~~~~~~~~~~~~~~~~~~\n"
                              + "1 errors, 0 warnings\n");
    }

    public void testMultipleCompositeDisposableMissingClear() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "public class Example {\n"
                + "  CompositeDisposable cd1;\n"
                + "  CompositeDisposable cd2;\n"
                + "  CompositeDisposable cd3;\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expect("src/foo/Example.java:4: Error: cd1 is never recycled. [sc.MissingCompositeDisposableRecycle]\n"
                              + "  CompositeDisposable cd1;\n"
                              + "  ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                              + "src/foo/Example.java:5: Error: cd2 is never recycled. [sc.MissingCompositeDisposableRecycle]\n"
                              + "  CompositeDisposable cd2;\n"
                              + "  ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                              + "src/foo/Example.java:6: Error: cd3 is never recycled. [sc.MissingCompositeDisposableRecycle]\n"
                              + "  CompositeDisposable cd3;\n"
                              + "  ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                              + "3 errors, 0 warnings\n"
                              + "");
    }

    public void testCompositeDisposableHavingClear() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "public class Example {\n"
                + "  CompositeDisposable cd;\n"
                + "  public void foo() {\n"
                + "   cd.clear();\n"
                + "  }\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expectClean();
    }

    public void testMultipleCompositeDisposableClear() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "public class Example {\n"
                + "  CompositeDisposable cd1;\n"
                + "  CompositeDisposable cd2;\n"
                + "  CompositeDisposable cd3;\n"
                + "  public void foo() {\n"
                + "   cd1.clear();\n"
                + "  }\n"
                + "  public void bar() {\n"
                + "   cd2.clear();\n"
                + "  }\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expect("src/foo/Example.java:6: Error: cd3 is never recycled. [sc.MissingCompositeDisposableRecycle]\n"
                              + "  CompositeDisposable cd3;\n"
                              + "  ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                              + "1 errors, 0 warnings\n");
    }

    // ISSUE_DISPOSE_COMPOSITE_DISPOSABLE

    public void testCallingCompositeDisposableDispose() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "public class Example {\n"
                + "  CompositeDisposable cd = new CompositeDisposable();\n"
                + "  public void foo() {\n"
                + "    cd.dispose();\n"
                + "  }\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expect("src/foo/Example.java:6: "
                              + "Warning: Usage of clear() instead of dispose(). [sc.DisposeCompositeDisposable]\n"
                              + "    cd.dispose();\n"
                              + "       ~~~~~~~\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testCallingCompositeDisposableDisposeSuppressed() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.CompositeDisposable;\n"
                + "public class Example {\n"
                + "  CompositeDisposable cd = new CompositeDisposable();\n"
                + "  @SuppressWarnings(\"sc.DisposeCompositeDisposable\") "
                + "  public void foo() {\n"
                + "    cd.dispose();\n"
                + "  }\n"
                + "}";
        lint().files(stubCompositeDisposable(), java(source))
              .run()
              .expectClean();
    }

    // Utils

    private static TestFile sampleRxSourceProvider() {
        return java("package foo;\n" +
                            "\n" +
                            "import io.reactivex.Completable;\n" +
                            "import io.reactivex.Maybe;\n" +
                            "import io.reactivex.Single;\n" +
                            "import io.reactivex.Observable;\n" +
                            "import io.reactivex.Flowable;\n" +
                            "import io.reactivex.disposables.Disposable;\n" +
                            "import io.reactivex.observers.TestObserver;\n" +
                            "import io.reactivex.subscribers.TestSubscriber;\n" +
                            "\n" +
                            "public class RxProvider {\n" +
                            "    \n" +
                            "    public static TestSubscriber<String> testSubscriber() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    public static TestObserver<String> testObserver() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    public static Disposable disposable() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    public static Completable completable() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    public static Maybe<String> maybe() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    public static Single<String> single() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    public static Observable<String> observable() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    public static Flowable<String> flowable() {\n" +
                            "        return null;\n" +
                            "    }    \n" +
                            "}");
    }

    private static TestFile sampleSuppressedRxSourceProvider() {
        return java("package foo;\n" +
                            "\n" +
                            "import io.reactivex.Completable;\n" +
                            "import io.reactivex.Maybe;\n" +
                            "import io.reactivex.Single;\n" +
                            "import io.reactivex.Observable;\n" +
                            "import io.reactivex.Flowable;\n" +
                            "import io.reactivex.disposables.Disposable;\n" +
                            "import io.reactivex.observers.TestObserver;\n" +
                            "import io.reactivex.subscribers.TestSubscriber;\n" +
                            "\n" +
                            "public class SuppressedRxProvider {\n" +
                            "    \n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static TestSubscriber<String> testSubscriber() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static TestObserver<String> testObserver() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static Disposable disposable() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static Completable completable() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static Maybe<String> maybe() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static Single<String> single() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static Observable<String> observable() {\n" +
                            "        return null;\n" +
                            "    }\n" +
                            "\n" +
                            "    @SuppressWarnings(\"sc.CheckResult\")" +
                            "    public static Flowable<String> flowable() {\n" +
                            "        return null;\n" +
                            "    }    \n" +
                            "}");
    }

    private static ProjectDescription getProject(@Language("JAVA") String source) {
        return new ProjectDescription(stubObservable(),
                                      stubCompletable(),
                                      stubMaybe(),
                                      stubSingle(),
                                      stubDisposable(),
                                      stubObservable(),
                                      stubTestSubscriber(),
                                      stubAction(),
                                      stubTestObserver(),
                                      stubFlowable(),
                                      stubConsumer(),
                                      sampleRxSourceProvider(),
                                      sampleSuppressedRxSourceProvider(),
                                      java(source));
    }
}
