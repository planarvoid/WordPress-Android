package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class EventLoggerParamsBuilder {

    private static final String TRIGGER_AUTO = "auto";
    private static final String TRIGGER_MANUAL = "manual";

    private Uri mOrigin;
    private long mSetId = Playable.NOT_SET;
    private int mSetPosition;
    private String mSource;
    private String mSourceVersion;
    private String mTrigger;

    public EventLoggerParamsBuilder(boolean manualPlay) {
        mTrigger = manualPlay ? TRIGGER_MANUAL : TRIGGER_AUTO;
    }

    public EventLoggerParamsBuilder origin(Uri origin) {
        mOrigin = origin;
        return this;
    }

    public EventLoggerParamsBuilder set(long setId, int position) {
        mSetId = setId;
        mSetPosition = position;
        return this;
    }

    public EventLoggerParamsBuilder source(String source) {
        mSource = source;
        return this;
    }

    public EventLoggerParamsBuilder sourceVersion(String version) {
        mSourceVersion = version;
        return this;
    }

    public String build() {
        final Uri.Builder builder = new Uri.Builder();

        builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.TRIGGER, mTrigger);

        final String originUrl = mOrigin.toString();
        if (ScTextUtils.isNotBlank(originUrl)) {
            builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.ORIGIN_URL, formatOriginUrl(originUrl));
        }

        if (ScTextUtils.isNotBlank(mSource)) {
            builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.SOURCE, mSource);
        }

        if (ScTextUtils.isNotBlank(mSourceVersion)) {
            builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.SOURCE_VERSION, mSourceVersion);
        }

        if (mSetId != Playable.NOT_SET) {
            builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.SET_ID, String.valueOf(mSetId));
            builder.appendQueryParameter(PlayEventTracker.EventLoggerKeys.SET_POSITION, String.valueOf(mSetPosition));
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
