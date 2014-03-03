package com.soundcloud.android.startup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;

import java.util.List;

public class StartupTaskExecutor extends ScheduledOperations {

    private static final String TAG = "StartupExecutor";
    private final List<? extends StartupTask> mTasks;
    private final Observer<Class<? extends StartupTask>> mObserver;

    public StartupTaskExecutor(){
        this(ScSchedulers.STORAGE_SCHEDULER, new DefaultSubscriber<Class<? extends StartupTask>>() {},
                new DeleteStreamCacheTask());
    }
    @VisibleForTesting
    protected StartupTaskExecutor(Scheduler scheduler,
                               Observer<Class<? extends StartupTask>> observer, StartupTask... tasks) {
        subscribeOn(scheduler);
        observeOn(scheduler);
        mTasks = Lists.newArrayList(tasks);
        mObserver = observer;
    }

    public void executeTasks() {
        schedule(Observable.create(new Observable.OnSubscribe<Class<? extends StartupTask>>() {
            //Sorry for the Observer<? super Class<? extends StartupTask>> but RxJava made me do it
            @Override
            public void call(Subscriber<? super Class<? extends StartupTask>> observer) {
                for (StartupTask task : mTasks) {
                    try{
                        task.executeTask();
                    } catch(Exception exception){
                        Log.e(TAG, String.format("%s threw %s : %s", task.getClass().getSimpleName(),
                                exception.getClass().getSimpleName(), exception.getMessage()));
                    }
                    observer.onNext(task.getClass());
                }
                observer.onCompleted();
            }
        })).subscribe(mObserver);
    }

    private static class DeleteStreamCacheTask implements StartupTask {
        @Override
        public void executeTask() {
            IOUtils.deleteDir(Consts.EXTERNAL_STREAM_DIRECTORY);
        }
    }
}
