
package com.soundcloud.android.task;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;

import android.util.Log;
import com.soundcloud.android.activity.ScCreate;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class PCMPlaybackTask extends AsyncTask<String, Integer, Boolean> {
    private File mPCMFile;

    private Boolean mPlaying;

    private PlaybackListener listener = null;

    private Thread writeThread;

    public AudioTrack playbackTrack;

    public PCMPlaybackTask(File pcmFile) {
        mPCMFile = pcmFile;

    }

    public void setPlaybackListener(PlaybackListener playbackListener) {
        listener = playbackListener;
    }

    public void stopPlayback() {
        mPlaying = false;

        if (playbackTrack != null && playbackTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
            playbackTrack.pause();

        if (writeThread != null && writeThread.isAlive())
            writeThread.interrupt();
    }

    @Override
    protected void onPreExecute() {
        mPlaying = true;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (listener != null)
            listener.onPlayProgressUpdate(progress[0]);

    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (writeThread.isAlive())
            writeThread.interrupt();

        playbackTrack.release();

        if (listener != null)
            listener.onPlayComplete(result);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            final int minSize = AudioTrack.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            playbackTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                    AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                    minSize, AudioTrack.MODE_STREAM);
            playbackTrack.play();

            writeThread = new Thread(new Runnable() {
                public void run() {
                    try {

                        InputStream is = new FileInputStream(mPCMFile);
                        BufferedInputStream bis = new BufferedInputStream(is);
                        DataInputStream dis = new DataInputStream(bis);

                        byte[] buffer = new byte[minSize];
                        while (dis.available() > 0) {
                            int bytesRead = 0;
                            while (dis.available() > 0 && bytesRead < minSize) {
                                buffer[bytesRead] = dis.readByte();
                                bytesRead++;
                            }
                            playbackTrack.write(buffer, 0, bytesRead);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "error", e);
                    }
                }
            });
            writeThread.start();

            while (mPlaying && playbackTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                publishProgress(playbackTrack.getPlaybackHeadPosition() * 2 * ScCreate.REC_CHANNELS);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
            }

            if (playbackTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
                playbackTrack.stop();

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // Define our custom Listener interface
    public interface PlaybackListener {
        public abstract void onPlayProgressUpdate(int position);

        public abstract void onPlayComplete(boolean result);
    }
}
