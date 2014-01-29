package com.soundcloud.android.robolectric;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.robolectric.shadows.ScShadowParcel;
import com.soundcloud.android.robolectric.shadows.ShadowV4Fragment;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.runners.model.InitializationError;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import rx.Subscription;
import rx.subjects.PublishSubject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

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
        SoundCloudApplication.instance.setAccountOperations(Mockito.mock(AccountOperations.class));
    }


    @Override
    public void afterTest(Method method) {
        super.afterTest(method);

        resetEventBusSubscriptions(method);
    }

    // hacky, but we need to ensure that forgetting to unsubscribe from an event queue does not interfere with other
    // tests due to stale observers still receiving messages and having knock-on effects.
    private void resetEventBusSubscriptions(Method method) {
        for (EventBus bus : EventBus.values()) {
            PublishSubject queue = bus.QUEUE;
            Class<? extends PublishSubject> clazz = queue.getClass();
            try {
                Field subscriptionManagerField = clazz.getDeclaredField("subscriptionManager");
                subscriptionManagerField.setAccessible(true);
                Object subscriptionManager = subscriptionManagerField.get(queue);

                Field stateField = subscriptionManager.getClass().getDeclaredField("state");
                stateField.setAccessible(true);
                AtomicReference subjectStateRef = (AtomicReference) stateField.get(subscriptionManager);
                Object subjectState = subjectStateRef.get();

                Class<?> subjectStateClass = subjectState.getClass();
                Field subscriptionsField = subjectStateClass.getDeclaredField("subscriptions");
                subscriptionsField.setAccessible(true);

                Subscription[] subscriptions = (Subscription[]) subscriptionsField.get(subjectState);

                // TODO: once we remove DefaultTestRunner, we should fail any test that forgets to unsubscribe!
//                if (subscriptions.length > 0) {
//                    Assert.fail("Test '" + method.getName() + "' has leaked subscriptions to " + bus.name() + " event queue;\n" +
//                        "Make sure you call `unsubscribe` after each test method!");
//                }
                for (Subscription s : subscriptions) {
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
