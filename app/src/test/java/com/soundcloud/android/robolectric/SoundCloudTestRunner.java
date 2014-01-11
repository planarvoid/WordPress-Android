package com.soundcloud.android.robolectric;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.robolectric.shadows.ScShadowParcel;
import com.soundcloud.android.robolectric.shadows.ShadowV4Fragment;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class SoundCloudTestRunner extends RobolectricTestRunner {

    public SoundCloudTestRunner(Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File("../app")));
    }

    @Override
    public void prepareTest(Object test) {
        super.prepareTest(test);
        MockitoAnnotations.initMocks(test);
    }

    @Override
    public void beforeTest(Method method) {
        super.beforeTest(method);

        // until we have a DI framework we have to set this instance to avoid NPEs
        SoundCloudApplication.instance = (SoundCloudApplication) Robolectric.application;
    }


    @Override
    public void afterTest(Method method) {
        super.afterTest(method);

        resetEventBusSubscriptions();
    }

    // hacky, but we need to ensure that forgetting to unsubscribe from an event queue does not interfere with other
    // tests due to stale observers still receiving messages and having knock-on effects.
    private void resetEventBusSubscriptions() {
        for (EventBus bus : EventBus.values()) {
            PublishSubject queue = bus.QUEUE;
            Class<? extends PublishSubject> clazz = queue.getClass();
            try {
                Field stateField = clazz.getDeclaredField("state");
                stateField.setAccessible(true);
                Object subjectState = stateField.get(queue);
                Class<?> subjectStateClass = subjectState.getClass();
                Field observersField = subjectStateClass.getDeclaredField("observers");
                observersField.setAccessible(true);
                Map<Subscription, Observer> observers = (Map<Subscription, Observer>) observersField.get(subjectState);

                for (Subscription s : observers.keySet()) {
                    System.err.println("Force unsubscribing from event queue " + bus.name() + "; forgot to call unsubscribe?");
                    s.unsubscribe();
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed resetting event queue " + bus.name() + ": " + e);
            }
        }
    }

    @Override
    protected void bindShadowClasses() {
        Robolectric.bindShadowClass(ScShadowParcel.class);
        Robolectric.bindShadowClass(ShadowV4Fragment.class);
    }
}
