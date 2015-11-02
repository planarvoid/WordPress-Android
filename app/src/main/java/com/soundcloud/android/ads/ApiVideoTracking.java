package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ApiVideoTracking {
    public final List<String> impressionUrls;
    public final List<String> skipUrls;

    public final List<String> startUrls;
    public final List<String> firstQuartileUrls;
    public final List<String> secondQuartileUrls;
    public final List<String> thirdQuartileUrls;
    public final List<String> finishUrls;

    public final List<String> pauseUrls;
    public final List<String> resumeUrls;
    public final List<String> fullScreenUrls;
    public final List<String> exitFullScreenUrls;

    @JsonCreator
    public ApiVideoTracking(@JsonProperty("impression_urls") List<String> impressionUrls,
                            @JsonProperty("skip_urls") List<String> skipUrls,
                            @JsonProperty("start_urls") List<String> startUrls,
                            @JsonProperty("first_quartile_urls") List<String> firstQuartileUrls,
                            @JsonProperty("second_quartile_urls") List<String> secondQuartileUrls,
                            @JsonProperty("third_quartile_urls") List<String> thirdQuartileUrls,
                            @JsonProperty("finish_urls") List<String> finishUrls,
                            @JsonProperty("pause_urls") List<String> pauseUrls,
                            @JsonProperty("resume_urls") List<String> resumeUrls,
                            @JsonProperty("fullscreen_urls") List<String> fullScreenUrls,
                            @JsonProperty("exit_fullscreen_urls") List<String> exitFullScreenUrls) {
        this.impressionUrls = impressionUrls;
        this.skipUrls = skipUrls;
        this.startUrls = startUrls;
        this.firstQuartileUrls = firstQuartileUrls;
        this.secondQuartileUrls = secondQuartileUrls;
        this.thirdQuartileUrls = thirdQuartileUrls;
        this.finishUrls = finishUrls;
        this.pauseUrls = pauseUrls;
        this.resumeUrls = resumeUrls;
        this.fullScreenUrls = fullScreenUrls;
        this.exitFullScreenUrls = exitFullScreenUrls;
    }
}
