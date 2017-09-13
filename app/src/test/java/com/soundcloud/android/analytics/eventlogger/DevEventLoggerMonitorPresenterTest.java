package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.playback.playqueue.SmoothScrollLinearLayoutManager;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.subjects.BehaviorSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowDialog;

import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class DevEventLoggerMonitorPresenterTest extends AndroidUnitTest {

    @Mock private SmoothScrollLinearLayoutManager layoutManager;
    @Mock private DevTrackingRecordsProvider trackingRecordsProvider;
    @Mock private DevTrackingRecordAdapter adapter;

    private final BehaviorSubject<DevTrackingRecordsProvider.Action> trackingRecordsAction = BehaviorSubject.create();

    private AppCompatActivity activity = activity();
    private DevEventLoggerMonitorPresenter presenter;

    @Before
    public void setUp() {
        activity.setContentView(R.layout.dev_event_logger_monitor_activity);
        presenter = new DevEventLoggerMonitorPresenter(layoutManager,
                                                       trackingRecordsProvider,
                                                       adapter);

        when(trackingRecordsProvider.action()).thenReturn(trackingRecordsAction);
    }

    @Test
    public void shouldDeleteAllTrackingRecordsOnDeleteAllButtonClick() {
        presenter.onCreate(activity, null);
        presenter.deleteAll.performClick();

        verify(trackingRecordsProvider).deleteAll();
    }

    @Test
    public void shouldReplaceItemsOnTrackingRecordsAction() {
        presenter.onCreate(activity, null);
        trackingRecordsAction.onNext(DevTrackingRecordsProvider.Action.ADD);

        verify(adapter).replaceItems(trackingRecordsProvider.latest());
    }

    @Test
    public void shouldShowDialogWithEventDataOnItemClicked() {
        presenter.onCreate(activity, null);
        presenter.onItemClicked(new TrackingRecord(123L, "backend", "data"));

        final TextView body = ShadowDialog.getLatestDialog().findViewById(R.id.body);
        assertThat(body.getText()).isEqualTo("data");
    }
}
