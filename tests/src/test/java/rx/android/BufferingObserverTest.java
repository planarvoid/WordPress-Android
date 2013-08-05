package rx.android;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rx.Observer;

@RunWith(JUnit4.class)
public class BufferingObserverTest {

    @Test
    public void shouldNotForwardItemsBeforeOnCompleteIsCalled() {
        Observer<Integer> wrapped = mock(Observer.class);
        BufferingObserver<Integer> bufferingObserver = new BufferingObserver<Integer>(wrapped);

        bufferingObserver.onNext(1);
        bufferingObserver.onNext(2);

        verify(wrapped, never()).onNext(anyInt());
    }

    @Test
    public void shouldForwardItemsWhenOnCompleteIsCalled() {
        Observer<Integer> wrapped = mock(Observer.class);
        BufferingObserver<Integer> bufferingObserver = new BufferingObserver<Integer>(wrapped);

        bufferingObserver.onNext(1);
        bufferingObserver.onNext(2);
        bufferingObserver.onCompleted();

        verify(wrapped, times(2)).onNext(anyInt());
        verify(wrapped, times(1)).onCompleted();
    }

    @Test
    public void shouldForwardOnErrorImmediately() {
        Observer<Integer> wrapped = mock(Observer.class);
        BufferingObserver<Integer> bufferingObserver = new BufferingObserver<Integer>(wrapped);

        Exception error = new Exception();
        bufferingObserver.onError(error);

        verify(wrapped).onError(error);
    }
}
