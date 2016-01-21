package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.UpsellStorage.UPSELL_DISMISSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

@SuppressLint("CommitPrefEdits")
@RunWith(MockitoJUnitRunner.class)
public class UpsellStorageTest {

    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;

    private UpsellStorage upsellStorage;
    private CurrentDateProvider dateProvider = new TestDateProvider();

    @Before
    public void setUp() {
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.clear()).thenReturn(editor);

        upsellStorage = new UpsellStorage(sharedPreferences, dateProvider);
    }

    @Test
    public void shouldClearData() {
        upsellStorage.clearData();

        InOrder inOrder = inOrder(editor);
        inOrder.verify(editor).clear();
        inOrder.verify(editor).apply();
    }

    @Test
    public void shouldBeAbleToDisplayUpsellIfNeverDismissed() {
        when(sharedPreferences.contains(UPSELL_DISMISSED)).thenReturn(false);

        assertThat(upsellStorage.canDisplayUpsell());
    }

    @Test
    public void shouldNotBeAbleToDisplayUpsellIfDismissedLessThan48hAgo() {
        when(sharedPreferences.contains(UPSELL_DISMISSED)).thenReturn(true);
        when(sharedPreferences.getLong(UPSELL_DISMISSED, dateProvider.getCurrentTime()))
                .thenReturn(yesterday());

        assertThat(upsellStorage.canDisplayUpsell()).isFalse();
    }

    @Test
    public void shouldBeAbleToDisplayUpsellIfDismissedMoreThan48hAgo() {
        when(sharedPreferences.contains(UPSELL_DISMISSED)).thenReturn(true);
        when(sharedPreferences.getLong(UPSELL_DISMISSED, dateProvider.getCurrentTime()))
                .thenReturn(theDayBeforeYesterday());

        assertThat(upsellStorage.canDisplayUpsell());
    }

    @Test
    public void shouldWriteToPreferencesWhenUpsellDismissed() {
        when(editor.putLong(UPSELL_DISMISSED, dateProvider.getCurrentTime())).thenReturn(editor);

        upsellStorage.setUpsellDismissed();

        InOrder inOrder = inOrder(editor);
        inOrder.verify(editor).putLong(UPSELL_DISMISSED, dateProvider.getCurrentTime());
        inOrder.verify(editor).apply();
    }

    private long yesterday() {
        return dateProvider.getCurrentTime() - TimeUnit.DAYS.toMillis(1);
    }

    private long theDayBeforeYesterday() {
        return dateProvider.getCurrentTime() - TimeUnit.DAYS.toMillis(2);
    }

}
