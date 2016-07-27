package com.soundcloud.android.upsell;

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
public class InlineUpsellStorageTest {

    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;

    private InlineUpsellStorage upsellStorage;
    private CurrentDateProvider dateProvider = new TestDateProvider();

    private String prefId = InlineUpsellStorage.upsellIdToPrefId("stream");

    @Before
    public void setUp() {
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.clear()).thenReturn(editor);

        upsellStorage = new InlineUpsellStorage(sharedPreferences, dateProvider);
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
        when(sharedPreferences.contains(prefId)).thenReturn(false);

        assertThat(upsellStorage.canDisplayUpsell("stream")).isTrue();
    }

    @Test
    public void shouldNotBeAbleToDisplayUpsellIfDismissedLessThan48hAgo() {
        when(sharedPreferences.contains(prefId)).thenReturn(true);
        when(sharedPreferences.getLong(prefId, dateProvider.getCurrentTime()))
                .thenReturn(yesterday());

        assertThat(upsellStorage.canDisplayUpsell("stream")).isFalse();
    }

    @Test
    public void shouldBeAbleToDisplayUpsellIfDismissedMoreThan48hAgo() {
        when(sharedPreferences.contains(prefId)).thenReturn(true);
        when(sharedPreferences.getLong(prefId, dateProvider.getCurrentTime()))
                .thenReturn(theDayBeforeYesterday());

        assertThat(upsellStorage.canDisplayUpsell("stream")).isTrue();
    }

    @Test
    public void shouldWriteToPreferencesWhenUpsellDismissed() {
        when(editor.putLong(prefId, dateProvider.getCurrentTime())).thenReturn(editor);

        upsellStorage.setUpsellDismissed("stream");

        InOrder inOrder = inOrder(editor);
        inOrder.verify(editor).putLong(prefId, dateProvider.getCurrentTime());
        inOrder.verify(editor).apply();
    }

    private long yesterday() {
        return dateProvider.getCurrentTime() - TimeUnit.DAYS.toMillis(1);
    }

    private long theDayBeforeYesterday() {
        return dateProvider.getCurrentTime() - TimeUnit.DAYS.toMillis(2);
    }

}
