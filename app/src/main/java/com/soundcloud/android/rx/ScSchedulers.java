package com.soundcloud.android.rx;

import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.ErrorUtils;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;


public final class ScSchedulers {

    @Deprecated
    /** Use {@link ScSchedulers#RX_HIGH_PRIORITY_SCHEDULER} */
    public static final rx.Scheduler HIGH_PRIO_SCHEDULER;
    @Deprecated
    /** Use {@link ScSchedulers#RX_LOW_PRIORITY_SCHEDULER} */
    public static final rx.Scheduler LOW_PRIO_SCHEDULER;

    public static final Scheduler RX_HIGH_PRIORITY_SCHEDULER;
    public static final Scheduler RX_LOW_PRIORITY_SCHEDULER;

    private static final long QUEUE_WAIT_WARNING_THRESHOLD = TimeUnit.SECONDS.toMillis(1);
    private static final long QUEUE_SIZE_WARNING_THRESHOLD = 3;

    static {
        final Executor highPriorityPool = createExecutor("HighPriorityPool", 6);
        final Executor lowPriorityPool = createExecutor("LowPriorityPool", 1);

        HIGH_PRIO_SCHEDULER = rx.schedulers.Schedulers.from(highPriorityPool);
        RX_HIGH_PRIORITY_SCHEDULER = Schedulers.from(highPriorityPool);
        LOW_PRIO_SCHEDULER = rx.schedulers.Schedulers.from(lowPriorityPool);
        RX_LOW_PRIORITY_SCHEDULER = Schedulers.from(lowPriorityPool);
    }

    private static Executor createExecutor(final String threadIdentifier, int numThreads) {

        return new WaitTimeMonitoringExecutorService((ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
            final AtomicLong counter = new AtomicLong();

            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, threadIdentifier + "-" + counter.incrementAndGet());
            }
        }));
    }

    private static class WaitTimeMonitoringExecutorService implements ExecutorService {

        private final ThreadPoolExecutor target;

        WaitTimeMonitoringExecutorService(ThreadPoolExecutor target) {
            this.target = target;
        }

        @Override
        public void execute(@NonNull final Runnable command) {
            logExecuteWarning();
            final long startTime = System.currentTimeMillis();

            if (ApplicationProperties.isBetaOrBelow()) {
                //We only track wait time for alpha, beta and debug.
                final WaitTimeTaskTrace waitTimeTaskTrace = new WaitTimeTaskTrace();
                waitTimeTaskTrace.startMeasuring();
                target.execute(() -> {
                    waitTimeTaskTrace.stopMeasuring();
                    logExecutingWarning(startTime);
                    command.run();
                });
            } else {
                target.execute(() -> {
                    logExecutingWarning(startTime);
                    command.run();
                });
            }
        }

        private void logExecuteWarning() {
            final int size = target.getQueue().size();
            if (size > QUEUE_SIZE_WARNING_THRESHOLD) {
                ErrorUtils.log(Log.WARN, OperationsInstrumentation.TAG, "Execute Command [queuedCount = " + size + "]");
            }
        }

        private void logExecutingWarning(long startTime) {
            final long waitTime = System.currentTimeMillis() - startTime;
            if (waitTime > QUEUE_WAIT_WARNING_THRESHOLD) {
                ErrorUtils.log(Log.WARN, OperationsInstrumentation.TAG, "Command Executed [waitTime = " + waitTime + "ms] ");
            }
        }

        @Override
        public void shutdown() {
            target.shutdown();
        }

        @NonNull
        @Override
        public List<Runnable> shutdownNow() {
            return target.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return target.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return target.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return target.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(final Callable<T> task) {
            return target.submit(task);
        }

        @Override
        public <T> Future<T> submit(final Runnable task, final T result) {
            return target.submit(task, result);
        }

        @Override
        public Future<?> submit(final Runnable task) {
            return target.submit(task);
        }

        @NonNull
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return target.invokeAll(tasks);
        }

        @NonNull
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                             long timeout,
                                             TimeUnit unit) throws InterruptedException {
            return target.invokeAll(tasks, timeout, unit);
        }

        @NonNull
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return target.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                               long timeout,
                               TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return target.invokeAny(tasks, timeout, unit);
        }
    }


    /**
     * <p>This class is meant to be used only HERE to measure how much time each task in the pool of threads waits
     * before execution.</p>
     *
     * <p>The reason of its existence is because {@link PerformanceMetricsEngine} is an injectable class (which should
     * be used for any performance measurement across the project) but at the time we instantiate our pool of threads
     * (at class loading time using blocks), our dependency injector is not ready.</p>
     *
     * <p>Refer to {@link PerformanceMetricsEngine}.</p>
     */
    private static class WaitTimeTaskTrace {
        private final Trace trace;

        private WaitTimeTaskTrace() {
            this.trace = FirebasePerformance.getInstance().newTrace(MetricType.DEV_THREAD_POOL_TASK_WAIT_TIME.toString());
        }

        private void startMeasuring() {
            if (trace != null) {
                trace.start();
            }
        }

        private void stopMeasuring() {
            if (trace != null) {
                trace.stop();
            }
        }
    }
}
