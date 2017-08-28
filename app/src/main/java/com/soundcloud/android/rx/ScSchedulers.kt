package com.soundcloud.android.rx

import android.util.Log
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.soundcloud.android.analytics.performance.MetricType
import com.soundcloud.android.properties.ApplicationProperties
import com.soundcloud.android.utils.ErrorUtils
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong


class ScSchedulers {
    companion object {
        private const val HIGH_PRIORITY_THREADS = 8
        private const val LOW_PRIORITY_THREADS = 1

        @Deprecated("Legacy RxJava 1x", ReplaceWith("RX_HIGH_PRIORITY_SCHEDULER"), DeprecationLevel.WARNING)
        @JvmField val HIGH_PRIO_SCHEDULER: rx.Scheduler
        @Deprecated("Legacy RxJava 1x", ReplaceWith("RX_LOW_PRIORITY_SCHEDULER"), DeprecationLevel.WARNING)
        @JvmField val LOW_PRIO_SCHEDULER: rx.Scheduler

        @JvmField val RX_HIGH_PRIORITY_SCHEDULER: Scheduler
        @JvmField val RX_LOW_PRIORITY_SCHEDULER: Scheduler

        init {
            val highPriorityExecutor = createExecutor("HighPriorityPool", HIGH_PRIORITY_THREADS)
            val lowPriorityExecutor = createExecutor("LowPriorityPool", LOW_PRIORITY_THREADS)

            HIGH_PRIO_SCHEDULER = rx.schedulers.Schedulers.from(highPriorityExecutor)
            LOW_PRIO_SCHEDULER = rx.schedulers.Schedulers.from(lowPriorityExecutor)
            RX_HIGH_PRIORITY_SCHEDULER = Schedulers.from(highPriorityExecutor)
            RX_LOW_PRIORITY_SCHEDULER = Schedulers.from(lowPriorityExecutor)
        }

        private fun createExecutor(threadIdentifier: String, numberOfThreads: Int): Executor {
            val executorThreadFactory = ExecutorThreadFactory(threadIdentifier)
            val executor = Executors.newFixedThreadPool(numberOfThreads, executorThreadFactory) as ThreadPoolExecutor
            return WaitTimeMonitoringExecutorService(executor)
        }
    }

    private class ExecutorThreadFactory(private val threadIdentifier: String) : ThreadFactory {
        private val counter = AtomicLong()

        override fun newThread(runnable: Runnable) = Thread(runnable, "$threadIdentifier-${counter.incrementAndGet()}")
    }

    private class WaitTimeMonitoringExecutorService(private val executor: ThreadPoolExecutor) : ExecutorService by executor {
        private val QUEUE_WAIT_WARNING_THRESHOLD = TimeUnit.SECONDS.toMillis(1)
        private val QUEUE_SIZE_WARNING_THRESHOLD = 3

        override fun execute(runnable: Runnable) {
            logExecuteWarning()
            val startTime = System.currentTimeMillis()

            if (ApplicationProperties.isBetaOrBelow()) {
                //We only track wait time for alpha, beta and debug.
                val waitTimeTaskTrace = WaitTimeTaskTrace()
                waitTimeTaskTrace.startMeasuring()
                executor.execute {
                    waitTimeTaskTrace.stopMeasuring()
                    logExecutingWarning(startTime)
                    runnable.run()
                }
            } else {
                executor.execute {
                    logExecutingWarning(startTime)
                    runnable.run()
                }
            }
        }

        private fun logExecuteWarning() {
            val size = executor.queue.size
            if (size > QUEUE_SIZE_WARNING_THRESHOLD) logWarning("Execute Command [queuedCount = $size]")
        }

        private fun logExecutingWarning(startTime: Long) {
            val waitTime = System.currentTimeMillis() - startTime
            if (waitTime > QUEUE_WAIT_WARNING_THRESHOLD) logWarning("Command Executed [waitTime = ${waitTime}ms]")
        }

        private fun logWarning(message: String) = ErrorUtils.log(Log.WARN, OperationsInstrumentation.TAG, message)
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
    private class WaitTimeTaskTrace {
        private val trace: Trace? = FirebasePerformance.getInstance()
                .newTrace(MetricType.DEV_THREAD_POOL_TASK_WAIT_TIME.toString())

        fun startMeasuring() = trace?.start()
        fun stopMeasuring() = trace?.stop()
    }
}
