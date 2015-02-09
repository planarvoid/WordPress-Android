package com.soundcloud.android.offline;

import com.soundcloud.android.crypto.EncryptionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.DeletePendingRemovalCommand;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import rx.functions.Action1;

import javax.inject.Inject;
import java.io.InputStream;

class DownloadOperations {
    private final StrictSSLHttpClient strictSSLHttpClient;
    private final SecureFileStorage fileStorage;
    private final DeletePendingRemovalCommand deleteOfflineContent;

    @Inject
    public DownloadOperations(StrictSSLHttpClient httpClient, SecureFileStorage fileStorage, DeletePendingRemovalCommand deleteOfflineContent) {
        this.strictSSLHttpClient = httpClient;
        this.fileStorage = fileStorage;
        this.deleteOfflineContent = deleteOfflineContent;
    }

    Action1<Object> deletePendingRemovals() {
        return deleteOfflineContent.toAction();
    }

    private void deleteTrack(Urn urn) {
        try {
            fileStorage.deleteTrack(urn);
        } catch (EncryptionException e1) {
            // note, in this case, the file probably didn't exist in the first place, so we are in a clean state
            Log.e(OfflineContentService.TAG, "Failed to remove file", e1);
        }
    }

    public DownloadResult download(DownloadRequest track) throws DownloadFailedException {
        InputStream input = null;
        try {
            input = strictSSLHttpClient.downloadFile(track.fileUrl);
            fileStorage.storeTrack(track.urn, input);

            Log.d(OfflineContentService.TAG, "Track stored on device: " + track.urn);
            return new DownloadResult(track.urn);
        } catch (Exception e) {
            deleteTrack(track.urn);
            throw new DownloadFailedException(track, e);
        } finally {
            IOUtils.close(input);
        }
    }

}
