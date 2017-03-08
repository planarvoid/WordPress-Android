package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class ApiAdTracking {
    public final List<String> clickUrls;
    public final List<String> impressionUrls;
    public final List<String> skipUrls;

    public final List<String> startUrls;
    public final List<String> firstQuartileUrls;
    public final List<String> secondQuartileUrls;
    public final List<String> thirdQuartileUrls;
    public final List<String> finishUrls;

    public final List<String> pauseUrls;
    public final List<String> resumeUrls;
    public final List<String> muteUrls;
    public final List<String> unmuteUrls;
    public final List<String> fullScreenUrls;
    public final List<String> exitFullScreenUrls;

    @JsonCreator
    public ApiAdTracking(@JsonProperty("click_urls") List<String> clickUrls,
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
        this.clickUrls = replaceNull(clickUrls);
        this.impressionUrls = replaceNull(impressionUrls);
        this.skipUrls = replaceNull(skipUrls);
        this.startUrls = replaceNull(startUrls);
        this.firstQuartileUrls = replaceNull(firstQuartileUrls);
        this.secondQuartileUrls = replaceNull(secondQuartileUrls);
        this.thirdQuartileUrls = replaceNull(thirdQuartileUrls);
        this.finishUrls = replaceNull(finishUrls);
        this.pauseUrls = replaceNull(pauseUrls);
        this.resumeUrls = replaceNull(resumeUrls);
        this.muteUrls = replaceNull(muteUrls);
        this.unmuteUrls = replaceNull(unmuteUrls);
        this.fullScreenUrls = replaceNull(fullScreenUrls);
        this.exitFullScreenUrls = replaceNull(exitFullScreenUrls);
    }

    private List<String> replaceNull(List<String> urls) {
        return urls == null ? Collections.emptyList() : urls;
    }
}
