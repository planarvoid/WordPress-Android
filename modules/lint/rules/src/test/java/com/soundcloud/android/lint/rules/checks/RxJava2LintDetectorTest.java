package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import org.intellij.lang.annotations.Language;

import java.util.Collections;
import java.util.List;

public class RxJava2LintDetectorTest extends BaseRxJava2LintDetectorTest {

    @Override
    protected Detector getDetector() {
        return new RxJava2LintDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(RxJava2LintDetector.ISSUE_METHOD_MISSING_CHECK_RESULT);
    }

    public void testMethodReturningObservableMissingCheckResultSuppressed() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Observable;\n"
                + "import java.lang.SuppressWarnings;\n"
                + "public class Example {\n"
                + "  @SuppressWarnings(\"sc.MissingCheckResult\") public Observable<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";

        lint().files(stubObservable(), stubConsumer(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningObservableMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Observable;\n"
                + "public class Example {\n"
                + "  public Observable<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubObservable(), stubConsumer(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public Observable<Object> foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningObservableHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Observable;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public Observable<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubObservable(), stubConsumer(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningFlowableMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Flowable;\n"
                + "public class Example {\n"
                + "  public Flowable<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubFlowable(), stubConsumer(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public Flowable<Object> foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningFlowableHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Flowable;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public Flowable<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubFlowable(), stubConsumer(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningSingleMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Single;\n"
                + "public class Example {\n"
                + "  public Single<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubSingle(), stubConsumer(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public Single<Object> foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningSingleHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Single;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public Single<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubSingle(), stubConsumer(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningMaybeMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Maybe;\n"
                + "public class Example {\n"
                + "  public Maybe<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubMaybe(), stubConsumer(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public Maybe<Object> foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningMaybeHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Maybe;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public Maybe<Object> foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubMaybe(), stubConsumer(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningCompletableMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Completable;\n"
                + "public class Example {\n"
                + "  public Completable foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubCompletable(), stubConsumer(), stubAction(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public Completable foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningCompletableHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.Completable;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public Completable foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubCompletable(), stubConsumer(), stubAction(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningDisposableMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.Disposable;\n"
                + "public class Example {\n"
                + "  public Disposable foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubDisposable(), stubConsumer(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public Disposable foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningDisposableHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.disposables.Disposable;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public Disposable foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubDisposable(), stubConsumer(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningTestObserverMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.observers.TestObserver;\n"
                + "public class Example {\n"
                + "  public TestObserver foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubTestObserver(), stubConsumer(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public TestObserver foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningTestObserverHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.observers.TestObserver;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public TestObserver foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubTestObserver(), stubConsumer(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }

    public void testMethodReturningTestSubscriberMissingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.subscribers.TestSubscriber;\n"
                + "public class Example {\n"
                + "  public TestSubscriber foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubTestSubscriber(), stubConsumer(), java(source))
              .run()
              .expect("src/foo/Example.java:4: "
                              + "Warning: Method should have @CheckResult annotation [sc.MissingCheckResult]\n"
                              + "  public TestSubscriber foo() {\n"
                              + "  ^\n"
                              + "0 errors, 1 warnings\n");
    }

    public void testMethodReturningTestSubscriberHavingCheckResult() throws Exception {
        @Language("JAVA") final String source = ""
                + "package foo;\n"
                + "import io.reactivex.subscribers.TestSubscriber;\n"
                + "import android.support.annotation.CheckResult;\n"
                + "public class Example {\n"
                + "  @CheckResult public TestSubscriber foo() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}";
        lint().files(stubTestSubscriber(), stubConsumer(), stubCheckResult(), java(source))
              .run()
              .expectClean();
    }
}
