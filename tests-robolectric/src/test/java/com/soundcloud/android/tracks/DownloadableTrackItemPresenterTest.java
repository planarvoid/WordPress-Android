package com.soundcloud.android.tracks;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
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
    @Mock private TrackItemMenuPresenter trackItemMenuPresenter;
    @Mock private FeatureOperations featureOperations;

    private DownloadableTrackItemPresenter presenter;

    @Before
    public void setup() {
        presenter = new DownloadableTrackItemPresenter(imageOperations, trackItemMenuPresenter, featureOperations);
    }

    @Test
    public void showsNoOfflineStateWhenNoOfflineProperties() {
        final PropertySet item = PropertySet.create();

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsPendingDownloadState() {
        final PropertySet item = PropertySet.from(TrackProperty.OFFLINE_REQUESTED_AT.bind(new Date()));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeTrue();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsPendingDownloadStateWhenResurrecting() {
        final PropertySet item = PropertySet.from(
                TrackProperty.OFFLINE_REMOVED_AT.bind(new Date(0)),
                TrackProperty.OFFLINE_REQUESTED_AT.bind(new Date()));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeTrue();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsDownloadingState() {
        final PropertySet item = PropertySet.from(
                TrackProperty.OFFLINE_DOWNLOADING.bind(true),
                TrackProperty.OFFLINE_REQUESTED_AT.bind(new Date()));

        expect(presenter.isDownloading(item)).toBeTrue();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsDownloadingStateWhenResurrecting() {
        final PropertySet item = PropertySet.from(
                TrackProperty.OFFLINE_REMOVED_AT.bind(new Date(0)),
                TrackProperty.OFFLINE_DOWNLOADING.bind(true),
                TrackProperty.OFFLINE_REQUESTED_AT.bind(new Date()));

        expect(presenter.isDownloading(item)).toBeTrue();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeFalse();
    }

    @Test
    public void showsDownloadedState() {
        final PropertySet item = PropertySet.from(TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeTrue();
    }

    @Test
    public void showsDownloadedStateWhenResurrecting() {
        final PropertySet item = PropertySet.from(
                TrackProperty.OFFLINE_REMOVED_AT.bind(new Date(0)),
                TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()));

        expect(presenter.isDownloading(item)).toBeFalse();
        expect(presenter.isPendingDownload(item)).toBeFalse();
        expect(presenter.isDownloaded(item)).toBeTrue();
    }
}