package com.soundcloud.android.startup;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class StartupTaskExecutorTest {

    private StartupTaskExecutor executor;
    @Mock
    private StartupTask taskOne;
    @Mock
    private StartupTask taskTwo;
    @Mock
    private Observer<Class<? extends StartupTask>> observer;

    @Before
    public void setup(){
        initMocks(this);
        executor = new StartupTaskExecutor(Schedulers.currentThread(),
                observer, taskOne, taskTwo);
    }

    @Test
    public void shouldRunSpecifiedTasksInOrder() throws Exception {
        executor.executeTasks();
        verifyInOrderTaskExecution();
    }

    @Test
    public void shouldNotifyObserverAfterRunningTasks() throws Exception {
        executor.executeTasks();
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Exception.class));
    }

    @Test
    public void shouldNotifyObserverAfterRunningTasksWithError() throws Exception {
        doThrow(NullPointerException.class).when(taskOne).executeTask();
        executor.executeTasks();
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Exception.class));
    }

    @Test
    public void shouldExecuteAllTasksEvenIfOneThrowsError(){
        doThrow(NullPointerException.class).when(taskOne).executeTask();
        executor.executeTasks();
        verifyInOrderTaskExecution();
    }

    private void verifyInOrderTaskExecution() {
        InOrder inOrder = inOrder(taskOne, taskTwo, observer);
        inOrder.verify(taskOne).executeTask();
        inOrder.verify(observer).onNext(any(Class.class));
        inOrder.verify(taskTwo).executeTask();
        inOrder.verify(observer).onNext(any(Class.class));
        verify(observer, never()).onError(any(Exception.class));
    }

}
