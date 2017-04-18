package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.List;

@AutoValue
abstract class ApiAdTracking {
    @JsonCreator
    public static ApiAdTracking create(@JsonProperty("click_urls") List<String> clickUrls,
                                       @JsonProperty("impression_urls") List<String> impressionUrls,
                                       @JsonProperty("skip_urls") List<String> skipUrls,
                                       @JsonProperty("start_urls") List<String> startUrls,
                                       @JsonProperty("first_quartile_urls") List<String> firstQuartileUrls,
                                       @JsonProperty("second_quartile_urls") List<String> secondQuartileUrls,
                                       @JsonProperty("third_quartile_urls") List<String> thirdQuartileUrls,
                                       @JsonProperty("finish_urls") List<String> finishUrls,
                                       @JsonProperty("pause_urls") List<String> pauseUrls,
                                       @JsonProperty("resume_urls") List<String> resumeUrls,
                                       @JsonProperty("mute") List<String> muteUrls,
                                       @JsonProperty("unmute") List<String> unmuteUrls,
                                       @JsonProperty("fullscreen_urls") List<String> fullScreenUrls,
                                       @JsonProperty("exit_fullscreen_urls") List<String> exitFullScreenUrls) {
        return new AutoValue_ApiAdTracking(replaceNull(clickUrls), replaceNull(impressionUrls),
                                           replaceNull(skipUrls), replaceNull(startUrls),
                                           replaceNull(firstQuartileUrls), replaceNull(secondQuartileUrls),
                                           replaceNull(thirdQuartileUrls), replaceNull(finishUrls),
                                           replaceNull(pauseUrls), replaceNull(resumeUrls),
                                           replaceNull(muteUrls), replaceNull(unmuteUrls),
                                           replaceNull(fullScreenUrls), replaceNull(exitFullScreenUrls));
    }

    public abstract List<String> clickUrls();
    public abstract List<String> impressionUrls();
    public abstract List<String> skipUrls();

    public abstract List<String> startUrls();
    public abstract List<String> firstQuartileUrls();
    public abstract List<String> secondQuartileUrls();
    public abstract List<String> thirdQuartileUrls();
    public abstract List<String> finishUrls();

    public abstract List<String> pauseUrls();
    public abstract List<String> resumeUrls();
    public abstract List<String> muteUrls();
    public abstract List<String> unmuteUrls();
    public abstract List<String> fullScreenUrls();
    public abstract List<String> exitFullScreenUrls();

    private static List<String> replaceNull(List<String> urls) {
        return urls == null ? Collections.emptyList() : urls;
    }
}
