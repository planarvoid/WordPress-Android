package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

@RunWith(SoundCloudTestRunner.class)
public class StateHolderFragmentTest {

    @Mock
    private Fragment hostFragment;
    @Mock
    private FragmentManager fragmentManager;

    @Before
    public void setup() {
        initMocks(this);
        when(hostFragment.getFragmentManager()).thenReturn(fragmentManager);
    }

    @Test
    public void shouldObtainExistingInstanceOfStateFragmentForHostFragment() {
        StateHolderFragment existingState = new StateHolderFragment();
        when(fragmentManager.findFragmentByTag("host_fragment_state")).thenReturn(existingState);

        StateHolderFragment stateHolderFragment = StateHolderFragment.obtain(fragmentManager, "host_fragment");
        expect(stateHolderFragment).toEqual(existingState);
    }

    @Test
    public void shouldStoreNewInstanceOfStateFragmentForHostFragment() {
        FragmentTransaction transaction = mock(FragmentTransaction.class);
        when(transaction.add(any(Fragment.class), anyString())).thenReturn(transaction);
        when(fragmentManager.beginTransaction()).thenReturn(transaction);

        StateHolderFragment.obtain(fragmentManager, "host_fragment");

        verify(transaction).add(any(StateHolderFragment.class), eq("host_fragment_state"));
    }

    @Test
    public void shouldReturnStoredValue() {
        StateHolderFragment state = new StateHolderFragment();
        state.put("data", 1);
        expect(state.getOrDefault("data", 0)).toBe(1);
    }

    @Test
    public void shouldReturnDefaultValue() {
        StateHolderFragment state = new StateHolderFragment();
        expect(state.getOrDefault("data", 1)).toBe(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseIllegalArgumentExceptionIfValueAtKeyNotOfRequiredType() {
        StateHolderFragment state = new StateHolderFragment();
        state.put("data", "string");
        state.getOrDefault("data", 1);
    }
}
