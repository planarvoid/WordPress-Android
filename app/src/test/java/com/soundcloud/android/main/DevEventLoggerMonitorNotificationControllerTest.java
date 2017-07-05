package com.soundcloud.android.main;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;

import java.util.Random;

public class DevEventLoggerMonitorNotificationControllerTest extends AndroidUnitTest {

    @Mock NotificationManager notificationManager;
    @Mock SharedPreferences sharedPreferences;
    @Mock SharedPreferences.Editor sharedPreferencesEditor;

    private DevEventLoggerMonitorNotificationController controller;

    @SuppressLint("CommitPrefEdits")
    @Before
    public void setUp() {
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putBoolean(anyString(), anyBoolean())).thenReturn(sharedPreferencesEditor);

        controller = new DevEventLoggerMonitorNotificationController(context(), notificationManager, sharedPreferences);
    }

    @Test
    public void shouldStartMonitoring() {
        controller.startMonitoring();

        verify(sharedPreferencesEditor).putBoolean("dev_event_logger_monitor_mute_key", false);
        verify(notificationManager).notify(eq(NotificationConstants.DEV_DRAWER_EVENT_LOGGER_MONITOR), any(Notification.class));
    }

    @Test
    public void shouldStopMonitoring() {
        controller.stopMonitoring();

        verify(sharedPreferencesEditor).putBoolean("dev_event_logger_monitor_mute_key", true);
        verify(notificationManager).cancel(NotificationConstants.DEV_DRAWER_EVENT_LOGGER_MONITOR);
    }

    @Test
    public void shouldSetMonitorMuteAction() {
        final boolean monitorMute = new Random().nextBoolean();
        controller.setMonitorMuteAction(monitorMute);
        verify(sharedPreferencesEditor).putBoolean("dev_event_logger_monitor_mute_key", monitorMute);
        verify(notificationManager).notify(eq(NotificationConstants.DEV_DRAWER_EVENT_LOGGER_MONITOR), any(Notification.class));
    }
}
