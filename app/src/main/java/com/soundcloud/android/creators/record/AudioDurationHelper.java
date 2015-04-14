package com.soundcloud.android.creators.record;

import com.soundcloud.android.Consts;

import android.media.MediaPlayer;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class AudioDurationHelper {

    private MediaPlayer mediaPlayer;

    @Inject
    public AudioDurationHelper() {
        // for dagger
    }

    public int getDuration(File file) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            return mediaPlayer.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Consts.NOT_SET;
    }
}
