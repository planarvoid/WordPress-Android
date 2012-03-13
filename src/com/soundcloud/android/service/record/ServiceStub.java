package com.soundcloud.android.service.record;

import com.soundcloud.android.model.Upload;

import android.os.RemoteException;

import java.lang.ref.WeakReference;

/*
* By making this a static class with a WeakReference to the Service, we
* ensure that the Service can be GCd even when the system process still has
* a remote reference to the stub.
*/
class ServiceStub extends ICloudCreateService.Stub {
    WeakReference<CloudCreateService> mService;

    public ServiceStub(CloudCreateService cloudUploaderService) {
        mService = new WeakReference<CloudCreateService>(cloudUploaderService);
    }

    @Override
    public void startRecording(String path, int mode) throws RemoteException {
        final CloudCreateService service = mService.get();
        if (service != null) service.startRecording(path, mode);
    }

    @Override
    public boolean isRecording() throws RemoteException {
        final CloudCreateService service = mService.get();
        return service != null && service.isRecording();
    }

    @Override
    public String getRecordingPath() throws RemoteException {
        final CloudCreateService service = mService.get();
        return service != null ? service.getRecordingPath() : null;
    }

    @Override
    public void stopRecording() throws RemoteException {
        final CloudCreateService service = mService.get();
        if (service != null) service.stopRecording();
    }

    @Override
    public void updateRecordTicker() throws RemoteException {
        final CloudCreateService service = mService.get();
        if (service != null) service.updateRecordTicker();
    }

    @Override
    public void loadPlaybackTrack(String playbackFile) {
        final CloudCreateService service = mService.get();
        if (service != null) service.loadPlaybackTrack(playbackFile);
    }

    @Override
    public boolean isPlayingBack() {
        final CloudCreateService service = mService.get();
        return service != null && service.isPlaying();
    }

    @Override
    public void startPlayback() {
        final CloudCreateService service = mService.get();
        if (service != null) service.startPlayback();
    }

    @Override
    public void pausePlayback() {
        final CloudCreateService service = mService.get();
        if (service != null) service.pausePlayback();
    }

    @Override
    public void stopPlayback() {
        final CloudCreateService service = mService.get();
        if (service != null) service.stopPlayback();
    }

    @Override
    public long getCurrentPlaybackPosition() {
        final CloudCreateService service = mService.get();
        return service != null ? service.getCurrentPlaybackPosition() : 0;
    }

    public float getCurrentProgressPercent() {
        final CloudCreateService service = mService.get();
        return service != null ? service.getCurrentProgressPercent() : 0f;
    }

    @Override
    public long getPlaybackDuration() {
        final CloudCreateService service = mService.get();
        return service != null ? service.getPlaybackDuration() : -1;
    }

    @Override
    public void seekTo(int position) {
        final CloudCreateService service = mService.get();
        if (service != null) service.seekTo(position);
    }

    @Override
    public boolean startUpload(Upload upload) throws RemoteException {
        final CloudCreateService service = mService.get();
        return (service != null) && service.startUpload(upload);

    }

    @Override
    public boolean isUploading() throws RemoteException {
        final CloudCreateService service = mService.get();
        return service != null && service.isUploading();
    }

    @Override
    public void cancelUpload() throws RemoteException {
        final CloudCreateService service = mService.get();
        if (service != null) service.cancelUpload();
    }

    @Override
    public void cancelUploadById(long id) throws RemoteException {
        final CloudCreateService service = mService.get();
        if (service != null) service.cancelUploadById(id);
    }

    @Override
    public String getPlaybackPath() throws RemoteException {
        final CloudCreateService service = mService.get();
        return service != null ? service.getCurrentPlaybackPath() : null;
    }

    @Override
    public Upload getUploadById(long id) throws RemoteException {
        final CloudCreateService service = mService.get();
        return service != null ? service.getUploadById(id) : null;
    }

    @Override
    public long getUploadLocalId() throws RemoteException {
        final CloudCreateService service = mService.get();
        return service != null ? service.getUploadLocalId() : 0;
    }

    @Override
    public long getPlaybackLocalId() throws RemoteException {
        final CloudCreateService service = mService.get();
        return service != null ? service.getPlaybackLocalId() : 0;
    }

}
