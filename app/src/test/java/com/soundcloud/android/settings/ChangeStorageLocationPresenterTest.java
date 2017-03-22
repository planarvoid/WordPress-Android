package com.soundcloud.android.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.offline.OfflineContentLocation;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowAlertDialog;
import rx.subjects.PublishSubject;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

public class ChangeStorageLocationPresenterTest extends AndroidUnitTest {

    @Mock OfflineSettingsStorage offlineSettingsStorage;
    @Mock OfflineContentOperations offlineContentOperations;
    @Mock EventBus eventBus;

    private AppCompatActivity activity = activity();
    private ChangeStorageLocationPresenter presenter;

    @Before
    public void setUp() {
        final PublishSubject<Void> offlineObservable = PublishSubject.create();
        when(offlineContentOperations.resetOfflineContent(any(OfflineContentLocation.class))).thenReturn(offlineObservable);

        activity.setContentView(R.layout.change_storage_location_activity);
        presenter = new ChangeStorageLocationPresenter(offlineSettingsStorage, offlineContentOperations, eventBus);
    }

    @Test
    public void shouldSetDeviceStorageChecked() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.DEVICE_STORAGE);

        presenter.onCreate(activity, null);

        assertThat(presenter.storageRadioButton.isChecked()).isTrue();
        assertThat(presenter.sdCardRadioButton.isChecked()).isFalse();
    }

    @Test
    public void shouldSetSDCardChecked() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.SD_CARD);

        presenter.onCreate(activity, null);

        assertThat(presenter.storageRadioButton.isChecked()).isFalse();
        assertThat(presenter.sdCardRadioButton.isChecked()).isTrue();
    }

    @Test
    public void shouldUpdateRadioGroupOnDialogCancel() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.DEVICE_STORAGE);

        presenter.onCreate(activity, null);
        presenter.sdCardRadioButton.setChecked(true);

        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        dialog.cancel();
        assertThat(presenter.storageRadioButton.isChecked()).isTrue();
    }

    @Test
    public void shouldUpdateRadioGroupOnDialogNegativeButtonClick() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.DEVICE_STORAGE);

        presenter.onCreate(activity, null);
        presenter.sdCardRadioButton.setChecked(true);
        assertThat(presenter.sdCardRadioButton.isChecked()).isTrue();

        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        Button negativeButton = (Button) dialog.findViewById(android.R.id.button2);
        negativeButton.performClick();
        assertThat(presenter.storageRadioButton.isChecked()).isTrue();
    }

    @Test
    public void shouldResetOfflineContentOnDialogPositiveButtonClick() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.DEVICE_STORAGE);

        presenter.onCreate(activity, null);
        presenter.sdCardRadioButton.setChecked(true);

        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        Button positiveButton = (Button) dialog.findViewById(android.R.id.button1);
        positiveButton.performClick();

        verify(offlineContentOperations).resetOfflineContent(OfflineContentLocation.SD_CARD);
    }

    @Test
    public void shouldSendScreenEventOnShowDialog() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.DEVICE_STORAGE);

        presenter.onCreate(activity, null);
        presenter.sdCardRadioButton.setChecked(true);

        verify(eventBus).publish(eq(EventQueue.TRACKING), any(ScreenEvent.class));
    }

    @Test
    public void shouldSendOfflineInteractionEventOnDialogPositiveButtonClick() {
        when(offlineSettingsStorage.getOfflineContentLocation()).thenReturn(OfflineContentLocation.DEVICE_STORAGE);

        presenter.onCreate(activity, null);
        presenter.sdCardRadioButton.setChecked(true);

        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        Button positiveButton = (Button) dialog.findViewById(android.R.id.button1);
        positiveButton.performClick();

        verify(eventBus).publish(eq(EventQueue.TRACKING), any(OfflineInteractionEvent.class));
    }
}
