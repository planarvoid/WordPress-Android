package com.soundcloud.android.offline;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;

public class DownloadController {

    private final DownloadHttpClient downloadHttpClient;
    private final SecureFileStorage fileStorage;
    private final TrackDownloadsStorage downloadsStorage;

    @Inject
    public DownloadController(DownloadHttpClient httpClient, SecureFileStorage fileStorage,
                              TrackDownloadsStorage downloadsStorage) {
        this.downloadHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.downloadsStorage = downloadsStorage;
    }

    public void downloadTracks() {
        final LinkedList<DownloadRequest> downloadRequests =
                new LinkedList<>(downloadsStorage.getPendingDownloads());

        fetchTracks(downloadRequests);
    }

    private void fetchTracks(Deque<DownloadRequest> tracks) {
        Log.d(OfflineContentService.TAG, "Started download of " + tracks.size() + " tracks");

        while (!tracks.isEmpty()) {
            DownloadResult result = fetchSingleTrack(tracks.pop());
            if (result.isSuccessful()) {
                downloadsStorage.updateDownload(result);
            }
        }
    }

    private DownloadResult fetchSingleTrack(DownloadRequest track) {
        Log.d(OfflineContentService.TAG, "Downloading track: " + track.urn + " from " + track.fileUrl);
        InputStream input = null;
        try {
            input = downloadHttpClient.downloadFile(track.fileUrl);
            fileStorage.storeTrack(track.urn, input);

            Log.d(OfflineContentService.TAG, "Track stored on device: " + track.urn);
            return DownloadResult.forSuccess(track.urn);

        } catch (IOException e) {
            Log.e(OfflineContentService.TAG, "Failed to download file", e);
        } catch (EncryptionException e) {
            Log.e(OfflineContentService.TAG, "Failed to encrypt file", e);
        } finally {
            IOUtils.close(input);
        }
        return DownloadResult.forFailure(track.urn);
    }
}
