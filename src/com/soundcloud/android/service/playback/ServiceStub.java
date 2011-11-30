package com.soundcloud.android.service.playback;

import com.soundcloud.android.model.Track;

import android.os.RemoteException;

import java.lang.ref.WeakReference;

/*
* By making this a static class with a WeakReference to the Service, we
* ensure that the Service can be GCd even when the system process still has
* a remote reference to the stub.
*/
class ServiceStub extends com.soundcloud.android.service.playback.ICloudPlaybackService.Stub {
    WeakReference<CloudPlaybackService> mService;

    ServiceStub(CloudPlaybackService service) {
        mService = new WeakReference<CloudPlaybackService>(service);
    }

    @Override
    public int getQueuePosition() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.mPlayListManager.getCurrentPosition() : 0;
    }

    @Override
    public void setQueuePosition(int index) {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.setQueuePosition(index);
    }

    @Override
    public boolean isPlaying() {
        CloudPlaybackService svc = mService.get();
        return svc != null && svc.isPlaying();
    }

    @Override
    public boolean isSupposedToBePlaying() {
        CloudPlaybackService svc = mService.get();
        return svc != null && svc.isSupposedToBePlaying();
    }

    @Override
    public boolean isSeekable() {
        CloudPlaybackService svc = mService.get();
        return svc != null && svc.isSeekable();
    }

    @Override
    public void stop() {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.stop();
    }

    @Override
    public void pause() {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.pause();
    }

    @Override
    public void toggle() throws RemoteException {
        CloudPlaybackService svc = mService.get();
        if (svc != null) {
            if (svc.isSupposedToBePlaying()) {
                svc.pause();
            } else {
                svc.play();
            }
        }
    }

    @Override
    public void play() {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.play();
    }

    @Override
    public void prev() {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.prev();
    }

    @Override
    public void next() {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.next();
    }

    @Override
    public void restart() {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.openCurrent();
    }

    @Override
    public String getTrackName() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.getTrackName() : null;
    }

    @Override
    public long getTrackId() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.getTrackId() : -1;
    }

    @Override
    public String getUserName() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.getUserName() : null;
    }

    @Override
    public String getUserPermalink() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.getUserPermalink() : null;
    }

    @Override
    public String getWaveformUrl() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.getWaveformUrl() : null;
    }

    @Override
    public boolean getDownloadable() {
        CloudPlaybackService svc = mService.get();
        return svc != null && svc.getDownloadable();
    }

    @Override
    public boolean isBuffering() {
        CloudPlaybackService svc = mService.get();
        return svc != null && svc.isBuffering();
    }

    @Override
    public long position() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.position() : 0;

    }

    @Override
    public long duration() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.getDuration() : 0;

    }

    @Override
    public int loadPercent() {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.loadPercent() : 0;
    }

    @Override
    public long seek(long pos, boolean perform) {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.seek(pos, perform) : 0;
    }

    @Override
    public Track getTrack() throws RemoteException {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.getTrack() : null;
    }

    @Override

    public Track getTrackAt(int pos) throws RemoteException {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.mPlayListManager.getTrackAt(pos) : null;
    }

    @Override
    public long getTrackIdAt(int pos) throws RemoteException {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.mPlayListManager.getTrackIdAt(pos) : -1;
    }

    @Override
    public int getQueueLength() throws RemoteException {
        CloudPlaybackService svc = mService.get();
        return svc != null ? svc.mPlayListManager.getCurrentLength() : 0;
    }

    @Override
    public void playFromAppCache(int playPos) throws RemoteException {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.playFromAppCache(playPos);
    }

    @Override
    public void setFavoriteStatus(long trackId, boolean favoriteStatus) throws RemoteException {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.setFavoriteStatus(trackId, favoriteStatus);
    }

    @Override
    public void setClearToPlay(boolean clearToPlay) throws RemoteException {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.setClearToPlay(clearToPlay);
    }

    @Override
    public void setAutoAdvance(boolean auto) throws RemoteException {
        CloudPlaybackService svc = mService.get();
        if (svc != null) svc.setAutoAdvance(auto);
    }
}
