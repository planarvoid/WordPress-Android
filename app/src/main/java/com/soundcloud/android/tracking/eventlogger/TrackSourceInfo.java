package com.soundcloud.android.tracking.eventlogger;

public class TrackSourceInfo extends ParamsMap {

    public static final TrackSourceInfo EMPTY = new TrackSourceInfo(0);

    private static final String KEY_SOURCE = "source";
    private static final String KEY_RECOMMENDER_VERSION = "source_version";
    private static final String KEY_TRIGGER = "trigger";

    private static final String SOURCE_RECOMMENDER = "recommender";
    private static final String TRIGGER_AUTO = "auto";
    private static final String TRIGGER_MANUAL = "manual";

    public TrackSourceInfo(int expectedSize) {
        super(expectedSize);
    }

    public static TrackSourceInfo fromRecommender(String recommenderVersion){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(2);
        trackSourceInfo.put(KEY_SOURCE, SOURCE_RECOMMENDER);
        trackSourceInfo.put(KEY_RECOMMENDER_VERSION, recommenderVersion);
        trackSourceInfo.put(KEY_TRIGGER, TRIGGER_AUTO);
        return trackSourceInfo;
    }

    public static TrackSourceInfo auto(){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(1);
        trackSourceInfo.put(KEY_TRIGGER, TRIGGER_AUTO);
        return trackSourceInfo;
    }

    public static TrackSourceInfo manual(){
        TrackSourceInfo trackSourceInfo = new TrackSourceInfo(1);
        trackSourceInfo.put(KEY_TRIGGER, TRIGGER_MANUAL);
        return trackSourceInfo;
    }

    public String getTrigger(){
        return get(KEY_TRIGGER);
    }

    public String getRecommenderVersion() {
        return get(KEY_RECOMMENDER_VERSION);
    }
}
