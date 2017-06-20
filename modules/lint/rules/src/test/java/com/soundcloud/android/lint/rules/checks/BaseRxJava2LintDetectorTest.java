package com.soundcloud.android.lint.rules.checks;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;

public abstract class BaseRxJava2LintDetectorTest extends LintDetectorTest {

    static LintDetectorTest.TestFile stubCompositeDisposable() {
        return java("package io.reactivex.disposables;\n"
                            + "public class CompositeDisposable {\n"
                            + "  public void dispose() {}\n"
                            + "  public void addAll() {}\n"
                            + "  public void clear() {}\n"
                            + "}");
    }

    static LintDetectorTest.TestFile stubConsumer() {
        return java("io/reactivex/functions/Consumer.java",
                    "package io.reactivex.functions;\n"
                            + "public interface Consumer<T> {\n"
                            + "  void accept(T t) throws Exception;\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubDisposable() {
        return java("io/reactivex/disposables/Disposable.java",
                    "package io.reactivex.disposables;\n"
                            + "public interface Disposable<T> {\n"
                            + "  boolean isDisposed();\n"
                            + "  void dispose();\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubTestObserver() {
        return java("io/reactivex/observers/TestObserver.java",
                    "package io.reactivex.observers;\n"
                            + "public interface TestObserver<T> {\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubTestSubscriber() {
        return java("io/reactivex/subscribers/TestSubscriber.java",
                    "package io.reactivex.subscribers;\n"
                            + "public interface TestSubscriber<T> {\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubAction() {
        return java("package io.reactivex.functions;\n"
                            + "public interface Action {\n"
                            + "  void run() throws Exception;\n"
                            + "}");
    }

    static LintDetectorTest.TestFile stubObservable() {
        return java("io/reactivex/Observable.java",
                    "package io.reactivex;\n"
                            + "import io.reactivex.functions.Consumer;\n"
                            + "public class Observable<T> {\n"
                            + "  public void subscribe() {}\n"
                            + "  public void subscribe(Consumer<T> onNext) {}\n"
                            + "  public void subscribe(Consumer<T> onNext, Consumer<Throwable> onError) {}\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubFlowable() {
        return java("io/reactivex/Flowable.java",
                    "package io.reactivex;\n"
                            + "import io.reactivex.functions.Consumer;\n"
                            + "public class Flowable<T> {\n"
                            + "  public void subscribe() {}\n"
                            + "  public void subscribe(Consumer<T> onNext) {}\n"
                            + "  public void subscribe(Consumer<T> onNext, Consumer<Throwable> onError) {}\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubSingle() {
        return java("io/reactivex/Single.java",
                    "package io.reactivex;\n"
                            + "import io.reactivex.functions.Consumer;\n"
                            + "public class Single<T> {\n"
                            + "  public void subscribe() {}\n"
                            + "  public void subscribe(Consumer<T> onSuccess) {}\n"
                            + "  public void subscribe(Consumer<T> onSuccess, Consumer<Throwable> onError) {}\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubMaybe() {
        return java("io/reactivex/Maybe.java",
                    "package io.reactivex;\n"
                            + "import io.reactivex.functions.Consumer;\n"
                            + "public class Maybe<T> {\n"
                            + "  public void subscribe() {}\n"
                            + "  public void subscribe(Consumer<T> onSuccess) {}\n"
                            + "  public void subscribe(Consumer<T> onSuccess, Consumer<Throwable> onError) {}\n"
                            + "}").within("src");
    }

    static LintDetectorTest.TestFile stubCompletable() {
        return java("package io.reactivex;\n"
                            + "import io.reactivex.functions.Action;\n"
                            + "import io.reactivex.functions.Consumer;\n"
                            + "public class Completable {\n"
                            + "  public void subscribe() {}\n"
                            + "  public void subscribe(Action onComplete) {}\n"
                            + "  public void subscribe(Action onComplete, Consumer<Throwable> onError) {}\n"
                            + "}");
    }
}
