package com.soundcloud.android.events;

public class AdDebugEvent extends TrackingEvent {

    public static final String ACTION_INSERTED_AUDIO_AD = "inserted_audio_ad";
    public static final String ACTION_AD_IGNORED = "ignored_audio_ad";
    public static final String ACTION_TRACKED_IMPRESSION = "tracked_audio_ad_impression";
    public static final String ACTION_CLEARED_AUDIO_AD = "cleared_audio_ad";
    public static final String ACTION_AD_WITH_INVALID_PROGRESS = "ad_with_invalid_progress";

    public AdDebugEvent(String action) {
        super(KIND_DEFAULT, System.currentTimeMillis());
        put("action", action);
    }

    public static AdDebugEvent insertedAudioAd(){
        return new AdDebugEvent(ACTION_INSERTED_AUDIO_AD);
    }

    public static AdDebugEvent ignoringAudioAd(){
        return new AdDebugEvent(ACTION_AD_IGNORED);
    }

    public static AdDebugEvent trackedAudioAdImpression(){
        return new AdDebugEvent(ACTION_TRACKED_IMPRESSION);
    }

    public static AdDebugEvent adWithInvalidProgress(){
        return new AdDebugEvent(ACTION_AD_WITH_INVALID_PROGRESS);
    }

    public static AdDebugEvent clearedAudioAd(){
        return new AdDebugEvent(ACTION_CLEARED_AUDIO_AD);
    }
}
