package com.soundcloud.android.tracking;

import com.at.ATParams;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

public class Media implements Event {
    public static final int REFRESH_MIN = 5000; // milliseconds

    final Track track;
    final int refreshInSeconds;
    final int durationInSeconds;
    final int fileSizeKB;

    Media(Track track) {
        this.track = track;
        durationInSeconds = track.duration / 1000;
        refreshInSeconds = refresh(track.duration) / 1000;
        fileSizeKB = track.getEstimatedFileSize() / 1024;
    }

    public static Media fromTrack(Track track) {
        if (track == null) return null;
        return new Media(track);
    }

    @Override public ATParams atParams(Object... args) {
        if (args == null || args.length == 0) throw new IllegalArgumentException("need action");
        ATParams.mediaAction maction;
        final Object arg = args[0];
        if (arg instanceof Action) {
            maction = ((Action)arg).action;
        } else if (arg instanceof String) {
            try {
                maction = ATParams.mediaAction.valueOf((String) arg);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else if (arg instanceof ATParams.mediaAction) {
            maction = (ATParams.mediaAction) arg;
        } else {
            throw new IllegalArgumentException("Illegal action parameter: "+ arg);
        }

        ATParams params = new ATParams();
        params.xt_rm(ATParams.mediaType.mediaTypeAudio,
                String.valueOf(Level2.Sounds.id),
                "",         /* player id */
                getMediaName(track),
                maction,
                String.valueOf(refreshInSeconds),
                String.valueOf(durationInSeconds),
                ATParams.mediaQuality.quality44khz,
                ATParams.mediaStream.mediaStream128kpbs,
                ATParams.mediaSource.sourceInt,
                false /* true = live, false = clip */,
                String.valueOf(fileSizeKB),
                ATParams.mediaExtension.mp3
                );
        return params;
    }

    @Override public Level2 level2() {
        return null;
    }

    /* package */ static String getMediaName(Track track) {
        User user = track.getUser();
        String userTrack = track.userTrackPermalink();
        return user == null ? userTrack  : user.permalink + "::" + userTrack;
    }

    /**
     * @param duration duration in msecs
     * @return refresh time in msecs
     */
    public static int refresh(int duration) {
        return Math.max(REFRESH_MIN, duration / 4);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+":"+getMediaName(track);
    }

    public enum Action {
        forward(ATParams.mediaAction.forward),
        backward(ATParams.mediaAction.backward),
        pause(ATParams.mediaAction.pause),
        play(ATParams.mediaAction.play),
        stop(ATParams.mediaAction.stop),
        refresh(ATParams.mediaAction.refresh);

        public final ATParams.mediaAction action;

        Action(ATParams.mediaAction action) {
            this.action = action;
        }
    }
}
