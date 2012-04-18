package com.soundcloud.android.activity.settings;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Actions;
import com.soundcloud.android.activity.settings.AlarmClock;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import com.xtremelabs.robolectric.shadows.ShadowAlarmManager;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class AlarmClockTests {
    ShadowAlarmManager shadowAlarmManager;
    AlarmClock alarm;

    @Before
    public void before() {
        AlarmManager amgr = (AlarmManager) Robolectric.application.getSystemService(Context.ALARM_SERVICE);
        shadowAlarmManager = shadowOf(amgr);
        alarm = new AlarmClock(Robolectric.application);
    }

    @Test
    @DisableStrictI18n
    public void shouldSetAlarm() throws Exception {
        alarm.set(10, 20);
        List<ShadowAlarmManager.ScheduledAlarm> alarms = shadowAlarmManager.getScheduledAlarms();
        expect(alarms.size()).toEqual(1);

        expect(alarms.get(0).type).toEqual(AlarmManager.RTC_WAKEUP);
        expect(shadowOf(alarms.get(0).operation).getSavedIntent().getAction()).toEqual(Actions.ALARM);

        List<Toast> toasts = shadowOf(Robolectric.application).getShownToasts();
        expect(toasts.size()).toEqual(1);
        expect(ShadowToast.getTextOfLatestToast()).toMatch("Alarm in \\d+ (seconds?|hours?|minutes?)");
    }


    @Test @DisableStrictI18n
    public void shouldSetAndCancelAnAlarmMessage() throws Exception {
        alarm.set(10, 20);
        String msg = Settings.System.getString(Robolectric.application.getContentResolver(),
                android.provider.Settings.System.NEXT_ALARM_FORMATTED);

        expect(msg).toMatch("Alarm set to \\d+:\\d+");

        alarm.cancel();

        msg = Settings.System.getString(Robolectric.application.getContentResolver(),
                android.provider.Settings.System.NEXT_ALARM_FORMATTED);

        expect(msg).toEqual("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfInvalidAlarmSpec() throws Exception {
        alarm.set(-1, 20);
    }

    @Test
    @DisableStrictI18n
    public void shouldCancelAlarm() throws Exception {
        alarm.set(10, 20);
        expect(shadowAlarmManager.getScheduledAlarms().size()).toEqual(1);
        alarm.cancel();
        expect(shadowAlarmManager.getScheduledAlarms().size()).toEqual(0);
    }

    @Test
    public void playShouldStartService() throws Exception {
        alarm.play(Robolectric.application, Content.ME_FAVORITES.uri);

        Intent intent = shadowOf(Robolectric.application).getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(CloudPlaybackService.PLAY);
        expect(intent.getData()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/favorites");
        expect(shadowOf(intent).getIntentClass().getSimpleName()).toEqual("CloudPlaybackService");
        expect(intent.getData().getQueryParameter(ScContentProvider.Parameter.CACHED)).toBeNull();
    }

    @Test
    public void playShouldUseCachedItemsIfNoConnection() throws Exception {
        TestHelper.simulateOffline();

        alarm.play(Robolectric.application, Content.ME_FAVORITES.uri);

        Intent intent = shadowOf(Robolectric.application).getNextStartedService();
        expect(intent).not.toBeNull();
        expect(intent.getData().getQueryParameter(ScContentProvider.Parameter.CACHED)).toEqual("1");
    }


    @Test
    public void receiverShouldScheduleAnotherAlarmIfFlightmodeSet() throws Exception {
        TestHelper.enableFlightmode(true);

        BroadcastReceiver receiver = new AlarmClock.Receiver();
        receiver.onReceive(Robolectric.application, new Intent(Actions.ALARM));

        expect(shadowOf(Robolectric.application).getNextStartedService()).toBeNull();
        expect(shadowAlarmManager.getScheduledAlarms().size()).toEqual(1);
    }

    @Test
    public void receiverShouldPlayIfFlightmodeNotSet() throws Exception {
        TestHelper.enableFlightmode(false);

        BroadcastReceiver receiver = new AlarmClock.Receiver();
        receiver.onReceive(Robolectric.application, new Intent(Actions.ALARM));

        expect(shadowOf(Robolectric.application).getNextStartedService()).not.toBeNull();
        expect(shadowAlarmManager.getScheduledAlarms().size()).toEqual(0);
    }

    @Test @DisableStrictI18n
    public void receiverShouldCancelAlarmOnCancelAction() throws Exception {
        alarm.set(10, 20);

        BroadcastReceiver receiver = new AlarmClock.Receiver();
        receiver.onReceive(Robolectric.application, new Intent(Actions.CANCEL_ALARM));

        expect(shadowAlarmManager.getScheduledAlarms().size()).toEqual(0);
    }
}
