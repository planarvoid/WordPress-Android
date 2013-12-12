package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
public class EventLoggerParamsBuilder {

    @Inject
    public EventLoggerParamsBuilder() {
    }

    public String build(TrackSourceInfo trackSourceInfo) {
        final Uri.Builder builder = new Uri.Builder();

        if (trackSourceInfo.getIsUserTriggered()){
            builder.appendQueryParameter(EventLoggerParams.Keys.TRIGGER, EventLoggerParams.Trigger.MANUAL);
        } else {
            builder.appendQueryParameter(EventLoggerParams.Keys.TRIGGER, EventLoggerParams.Trigger.AUTO);
        }
        builder.appendQueryParameter(EventLoggerParams.Keys.ORIGIN_URL, formatOriginUrl(trackSourceInfo.getOriginScreen().toString()));

        if (trackSourceInfo.hasSource()) {
            builder.appendQueryParameter(EventLoggerParams.Keys.SOURCE, trackSourceInfo.getSource());
            builder.appendQueryParameter(EventLoggerParams.Keys.SOURCE_VERSION, trackSourceInfo.getSourceVersion());
        }

        if (trackSourceInfo.isFromPlaylist()) {
            builder.appendQueryParameter(EventLoggerParams.Keys.SET_ID, String.valueOf(trackSourceInfo.getPlaylistId()));
            builder.appendQueryParameter(EventLoggerParams.Keys.SET_POSITION, String.valueOf(trackSourceInfo.getPlaylistPosition()));
        }
        return builder.build().getQuery().toString();
    }

    private String formatOriginUrl(String originUrl) {
        try {
            return URLEncoder.encode(originUrl.toLowerCase(Locale.ENGLISH).replace(" ", "_"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ScTextUtils.EMPTY_STRING;
    }
}
