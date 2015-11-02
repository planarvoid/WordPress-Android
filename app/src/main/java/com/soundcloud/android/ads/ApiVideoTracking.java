package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

public class ApiVideoTracking implements PropertySetSource {
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

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                VideoAdProperty.AD_IMPRESSION_URLS.bind(impressionUrls),
                VideoAdProperty.AD_SKIP_URLS.bind(skipUrls),
                VideoAdProperty.AD_START_URLS.bind(startUrls),
                VideoAdProperty.AD_FIRST_QUARTILE_URLS.bind(firstQuartileUrls),
                VideoAdProperty.AD_SECOND_QUARTILE_URLS.bind(secondQuartileUrls),
                VideoAdProperty.AD_THIRD_QUARTILE_URLS.bind(thirdQuartileUrls),
                VideoAdProperty.AD_FINISH_URLS.bind(finishUrls),
                VideoAdProperty.AD_PAUSE_URLS.bind(pauseUrls),
                VideoAdProperty.AD_RESUME_URLS.bind(resumeUrls),
                VideoAdProperty.AD_FULLSCREEN_URLS.bind(fullScreenUrls),
                VideoAdProperty.AD_EXIT_FULLSCREEN_URLS.bind(exitFullScreenUrls)
        );
    }
}
