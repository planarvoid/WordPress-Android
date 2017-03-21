package com.soundcloud.android.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.OfflineContentLocation;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;

public class ChangeStorageLocationPresenterTest extends AndroidUnitTest {

    @Mock OfflineSettingsStorage offlineSettingsStorage;
    @Mock OfflineContentOperations offlineContentOperations;

    private AppCompatActivity activity = activity();
    private ChangeStorageLocationPresenter presenter;

    @Before
    public void setUp() {
        activity.setContentView(R.layout.change_storage_location_activity);
        presenter = new ChangeStorageLocationPresenter(offlineSettingsStorage, offlineContentOperations);
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
}
