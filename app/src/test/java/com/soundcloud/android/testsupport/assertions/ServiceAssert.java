package com.soundcloud.android.testsupport.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowService;

import android.app.Service;
import android.content.BroadcastReceiver;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Custom assertion class for testing Android services.
 */
public final class ServiceAssert extends AbstractAssert<ServiceAssert, Service> {

    public ServiceAssert(Service actual) {
        super(actual, ServiceAssert.class);
    }

    public ServiceAssert hasStoppedSelf() {
        isNotNull();

        final String assertErrorMessage = String.format("Service <%s> was not stop by self.",
                actual.getClass().getSimpleName());

        ShadowService shadowService = Shadows.shadowOf(actual);
        assertThat(shadowService.isStoppedBySelf())
                .overridingErrorMessage(assertErrorMessage, actual)
                .isTrue();

        return this;
    }

    public ServiceAssert doesNotHaveLastForegroundNotification() {
        isNotNull();

        final String assertErrorMessage = String.format("Service <%s> has last foreground notification.",
                actual.getClass().getSimpleName());

        ShadowService shadowService = Shadows.shadowOf(actual);
        assertThat(shadowService.getLastForegroundNotification())
                .overridingErrorMessage(assertErrorMessage)
                .isNull();

        return this;
    }

    public ServiceAssert hasRegisteredReceiverWithAction(BroadcastReceiver receiver, String action) {
        isNotNull();
        if (receiver == null || action == null) {
            failWithMessage("Neither receiver nor action parameters cannot be null");
        }

        final String assertErrorMessage = String.format("<%s> does not contain broadcast receiver with action <%s>",
                actual.getClass().getSimpleName(), action);

        assertThat(getServiceReceiversForAction(action))
                .overridingErrorMessage(assertErrorMessage)
                .containsExactly(receiver);

        return this;
    }

    private ArrayList<BroadcastReceiver> getServiceReceiversForAction(String action) {
        ArrayList<BroadcastReceiver> broadcastReceivers = new ArrayList<>();
        for (ShadowApplication.Wrapper registeredReceiver : ShadowApplication.getInstance().getRegisteredReceivers()) {
            if (registeredReceiver.context == actual) {
                Iterator<String> actions = registeredReceiver.intentFilter.actionsIterator();
                while (actions.hasNext()) {
                    if (actions.next().equals(action)) {
                        broadcastReceivers.add(registeredReceiver.broadcastReceiver);
                    }
                }
            }

        }
        return broadcastReceivers;
    }
}
