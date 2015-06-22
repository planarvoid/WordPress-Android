package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.BroadcastReceiver;
import android.content.Context;

import java.util.Arrays;
import java.util.HashMap;

@RunWith(DefaultTestRunner.class)
public class AndroidUtilsTest {

    @Test
    public void shouldReturnKeysSortedByValue() throws Exception {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("d",1);
        map.put("b",3);
        map.put("a",4);
        map.put("c",2);

        final String[] actual = AndroidUtils.returnKeysSortedByValue(map);
        final String[] expected = {"a", "b", "c", "d"};

        expect(Arrays.equals(actual, expected)).toBeTrue();
    }

    @Test
    public void safeUnregisterReceiverShouldAllowNullReferences() {
        Context context = mock(Context.class);
        AndroidUtils.safeUnregisterReceiver(context, null);
        verify(context, never()).unregisterReceiver(null);
    }

    @Test
    public void safeUnregisterReceiverShouldNotThrowWhenReceiverAlreadyUnregistered() {
        Context context = mock(Context.class);
        BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        doThrow(new IllegalArgumentException("Receiver not registered")).when(context).unregisterReceiver(receiver);

        AndroidUtils.safeUnregisterReceiver(context, receiver);
    }

    @Test
    public void safeUnregisterReceiverShouldUnregisterReceiver() {
        Context context = mock(Context.class);
        BroadcastReceiver receiver = mock(BroadcastReceiver.class);
        AndroidUtils.safeUnregisterReceiver(context, receiver);
        verify(context).unregisterReceiver(receiver);
    }
}