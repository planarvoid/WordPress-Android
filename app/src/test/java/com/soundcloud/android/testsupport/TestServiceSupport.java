package com.soundcloud.android.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowService;

import android.app.Service;
import android.content.BroadcastReceiver;

import java.util.ArrayList;
import java.util.Iterator;

public class TestServiceSupport {

    private TestServiceSupport() {}

    public static void assertLastForegroundNotificationNull(Service service) {
        ShadowService shadowService = Shadows.shadowOf(service);
        assertThat(shadowService.getLastForegroundNotification()).isNull();
    }

    public static void assertServicesIsStoppedBySelf(Service service) {
        ShadowService shadowService = Shadows.shadowOf(service);
        assertThat(shadowService.isStoppedBySelf()).isTrue();
    }

    public static ArrayList<BroadcastReceiver> getServiceReceiversForAction(Service service, String action) {
        ArrayList<BroadcastReceiver> broadcastReceivers = new ArrayList<>();
        for (ShadowApplication.Wrapper registeredReceiver : ShadowApplication.getInstance().getRegisteredReceivers()) {
            if (registeredReceiver.context == service) {
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
