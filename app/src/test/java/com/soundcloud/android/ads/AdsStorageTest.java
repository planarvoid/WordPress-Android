package com.soundcloud.android.ads;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class AdsStorageTest {

    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;

    private CurrentDateProvider dateProvider = new TestDateProvider();
    private CurrentDateProvider dateProviderSpy;

    private AdsStorage adsStorage;
    private String KEY = "last_prestitial_fetch";

    @Before
    public void setUp() throws Exception {
        when(sharedPreferences.edit()).thenReturn(editor);
        dateProviderSpy = spy(dateProvider);
        adsStorage = new AdsStorage(sharedPreferences, dateProviderSpy);
    }

    @Test
    public void shouldShowPrestitialReturnsFalseIfFetchIntervalHasNotPassed() {
        setPrestitialFetchMinutesAgo(1);

        assertThat(adsStorage.shouldShowPrestitial()).isFalse();
    }

    @Test
    public void shouldShowPrestitialReturnsTrueIfFetchIntervalHasPassed() {
        setPrestitialFetchMinutesAgo(40);

        assertThat(adsStorage.shouldShowPrestitial()).isTrue();
    }

    @Test
    public void preventPrestitialFetchForInterval() {
        final long fakeTime = 123L;
        when(dateProviderSpy.getCurrentTime()).thenReturn(fakeTime);
        when(editor.putLong(KEY, fakeTime)).thenReturn(editor);

        adsStorage.preventPrestitialFetchForTimeInterval();

        verify(editor).putLong(KEY, fakeTime);
        verify(editor).apply();
    }

    private void setPrestitialFetchMinutesAgo(int minutes) {
        when(sharedPreferences.getLong(eq(KEY), anyLong())).thenReturn(minutesAgo(minutes));
    }

    private long minutesAgo(int minutes) {
        return dateProvider.getCurrentTime() - TimeUnit.MINUTES.toMillis(minutes);
    }
}
