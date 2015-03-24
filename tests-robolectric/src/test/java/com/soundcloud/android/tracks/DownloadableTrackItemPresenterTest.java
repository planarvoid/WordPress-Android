package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class DownloadableTrackItemPresenterTest {

    @Mock private ImageOperations imageOperations;
    @Mock private FeatureOperations featureOperations;

    private DownloadableTrackItemPresenter presenter;

    @Before
    public void setup() {
        presenter = new DownloadableTrackItemPresenter(null, null, featureOperations);
    }

    @Test
    public void showsNoOfflineStateWhenNoOfflineProperties() {
        final TrackItem item = TrackItem.from(PropertySet.create());

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsPendingDownloadState() {
        final TrackItem item = TrackItem.from(PropertySet.from(OfflineProperty.REQUESTED_AT.bind(new Date())));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeTrue();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsPendingDownloadStateWhenResurrecting() {
        final TrackItem item = TrackItem.from(PropertySet.from(
                OfflineProperty.REMOVED_AT.bind(new Date(0)),
                OfflineProperty.REQUESTED_AT.bind(new Date())));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeTrue();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsDownloadingState() {
        final TrackItem item = TrackItem.from(PropertySet.from(
                OfflineProperty.DOWNLOADING.bind(true),
                OfflineProperty.REQUESTED_AT.bind(new Date())));

        expect(presenter.isDownloading(item)).toBeTrue();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsDownloadingStateWhenResurrecting() {
        final TrackItem item = TrackItem.from(PropertySet.from(
                OfflineProperty.REMOVED_AT.bind(new Date(0)),
                OfflineProperty.DOWNLOADING.bind(true),
                OfflineProperty.REQUESTED_AT.bind(new Date())));

        expect(presenter.isDownloading(item)).toBeTrue();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsDownloadedState() {
        final TrackItem item = TrackItem.from(
                PropertySet.from(OfflineProperty.DOWNLOADED_AT.bind(new Date())));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeTrue();
    }

    @Test
    public void showsDownloadedStateWhenResurrecting() {
        final TrackItem item = TrackItem.from(PropertySet.from(
                OfflineProperty.REMOVED_AT.bind(new Date(0)),
                OfflineProperty.DOWNLOADED_AT.bind(new Date())));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeTrue();
    }
}