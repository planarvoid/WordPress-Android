package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import java.util.HashMap;
import java.util.Locale;

class EventLoggerEventData {

    @JsonProperty("event") final String event;
    @JsonProperty("version") final String version;
    @JsonProperty("payload") final HashMap<String, Object> payload;

    public EventLoggerEventData(String event,
                                String version,
                                int clientId,
                                String anonymousId,
                                String loggedInUserUrn,
                                long timestamp) {
        this.event = event;
        this.version = version;
        this.payload = new HashMap<>();
        addToPayload(CLIENT_ID, clientId);
        addToPayload(ANONYMOUS_ID, anonymousId);
        addToPayload(USER, loggedInUserUrn);
        addToPayload(TIMESTAMP, timestamp);
    }

    public EventLoggerEventData pageName(String pageName) {
        addToPayload(PAGE_NAME, pageName);
        return this;
    }

    public EventLoggerEventData pageUrn(String pageUrn) {
        addToPayload(PAGE_URN, pageUrn);
        return this;
    }

    public EventLoggerEventData adUrn(String adUrn) {
        addToPayload(AD_URN, adUrn);
        return this;
    }

    public EventLoggerEventData policy(String policy) {
        addToPayload(POLICY, policy);
        return this;
    }

    public EventLoggerEventData clickName(String clickName) {
        addToPayload(CLICK_NAME, clickName);
        return this;
    }


    public EventLoggerEventData clickTarget(String clickTarget) {
        addToPayload(CLICK_TARGET, clickTarget);
        return this;
    }

    public EventLoggerEventData clickObject(String clickObject) {
        addToPayload(CLICK_OBJECT, clickObject);
        return this;
    }

    public EventLoggerEventData clickCategory(String clickCategory) {
        addToPayload(CLICK_CATEGORY, clickCategory);
        return this;
    }

    public EventLoggerEventData externalMedia(String externalMedia) {
        addToPayload(EXTERNAL_MEDIA, externalMedia);
        return this;
    }

    public EventLoggerEventData monetizationModel(String monetizationModel) {
        addToPayload(MONETIZATION_MODEL, monetizationModel);
        return this;
    }

    public EventLoggerEventData monetizedObject(String monetizedObject) {
        addToPayload(MONETIZED_OBJECT, monetizedObject);
        return this;
    }

    public EventLoggerEventData monetizationType(String monetizationType) {
        addToPayload(MONETIZATION_TYPE, monetizationType);
        return this;
    }

    public EventLoggerEventData impressionName(String impressionName) {
        addToPayload(IMPRESSION_NAME, impressionName);
        return this;
    }

    public EventLoggerEventData impressionObject(String impressionObject) {
        addToPayload(IMPRESSION_OBJECT, impressionObject);
        return this;
    }

    public EventLoggerEventData impressionCategory(String impressionCategory) {
        addToPayload(IMPRESSION_CATEGORY, impressionCategory);
        return this;
    }

    public EventLoggerEventData inForeground(boolean inForeground) {
        addToPayload(IN_FOREGROUND, inForeground);
        return this;
    }

    public EventLoggerEventData adsRequested(boolean adsRequested) {
        addToPayload(AD_REQUEST_MADE, adsRequested);
        return this;
    }

    public EventLoggerEventData adsEndpoint(String adsEndpoint) {
        addToPayload(AD_REQUEST_ENDPOINT, adsEndpoint);
        return this;
    }

    public EventLoggerEventData adsRequestSuccess(boolean requestSuccessful) {
        addToPayload(AD_REQUEST_SUCCESS, requestSuccessful);
        return this;
    }

    public EventLoggerEventData playerVisible(boolean playerVisible) {
        addToPayload(PLAYER_VISIBLE, playerVisible);
        return this;
    }

    public EventLoggerEventData adOptimized(boolean optimized) {
        addToPayload(AD_SELECTION_OPTIMIZED, optimized);
        return this;
    }

    public EventLoggerEventData adsReceived(String adsReceived) {
        addToPayload(ADS_RECEIVED, adsReceived);
        return this;
    }

    public EventLoggerEventData trackLength(long length) {
        addToPayload(TRACK_LENGTH, length);
        return this;
    }

    public EventLoggerEventData trigger(String trigger) {
        addToPayload(TRIGGER, trigger);
        return this;
    }

    public EventLoggerEventData protocol(String protocol) {
        addToPayload(PROTOCOL, protocol);
        return this;
    }

    public EventLoggerEventData playerType(String playa) {
        addToPayload(PLAYER_TYPE, playa);
        return this;
    }

    public EventLoggerEventData uuid(String uuid) {
        addToPayload(EventLoggerParam.UUID, uuid);
        return this;
    }

    public EventLoggerEventData source(String source) {
        addToPayload(SOURCE, source);
        return this;
    }

    public EventLoggerEventData sourceUrn(String sourceUrn) {
        addToPayload(SOURCE_URN, sourceUrn);
        return this;
    }

    public EventLoggerEventData sourceVersion(String version) {
        addToPayload(SOURCE_VERSION, version);
        return this;
    }

    public EventLoggerEventData inPlaylist(Urn playlistUrn) {
        addToPayload(IN_PLAYLIST, String.valueOf(playlistUrn));
        return this;
    }

    public EventLoggerEventData track(Urn trackUrn) {
        addToPayload(TRACK, String.valueOf(trackUrn));
        return this;
    }

    public EventLoggerEventData trackOwner(Urn trackOwner) {
        addToPayload(TRACK_OWNER, String.valueOf(trackOwner));
        return this;
    }

    public EventLoggerEventData reposter(Urn reposter) {
        addToPayload(REPOSTER, String.valueOf(reposter));
        return this;
    }

    public EventLoggerEventData localStoragePlayback(boolean isLocalStoragePlayback) {
        addToPayload(LOCAL_STORAGE_PLAYBACK, isLocalStoragePlayback);
        return this;
    }

    public EventLoggerEventData consumerSubsPlan(Plan plan) {
        addToPayload(CONSUMER_SUBS_PLAN, plan.planId);
        return this;
    }

    public EventLoggerEventData playlistPosition(int position) {
        addToPayload(PLAYLIST_POSITION, position);
        return this;
    }

    public EventLoggerEventData latency(long latency) {
        addToPayload(LATENCY, String.valueOf(latency));
        return this;
    }

    public EventLoggerEventData type(String type) {
        addToPayload(TYPE, type);
        return this;
    }

    public EventLoggerEventData metric(String name, long value) {
        addToPayload(METRIC_NAME, name);
        addToPayload(METRIC_VALUE, value);
        return this;
    }

    public EventLoggerEventData mediaType(String type) {
        addToPayload(MEDIA_TYPE, type);
        return this;
    }

    public EventLoggerEventData host(String host) {
        addToPayload(HOST, host);
        return this;
    }

    public EventLoggerEventData os(String osVersion) {
        addToPayload(OS, osVersion);
        return this;
    }

    public EventLoggerEventData bitrate(String bitrate) {
        addToPayload(BITRATE, bitrate);
        return this;
    }

    public EventLoggerEventData bitrate(int bitrate) {
        addToPayload(BITRATE, bitrate);
        return this;
    }

    public EventLoggerEventData format(String format) {
        addToPayload(FORMAT, format.toLowerCase(Locale.US));
        return this;
    }

    public EventLoggerEventData errorName(String errorName) {
        addToPayload(ERROR_NAME, errorName);
        return this;
    }

    public EventLoggerEventData errorCode(String errorCode) {
        addToPayload(ERROR_CODE, errorCode);
        return this;
    }

    public EventLoggerEventData url(String url) {
        addToPayload(URL, url);
        return this;
    }

    public EventLoggerEventData queryUrn(String queryUrn) {
        addToPayload(QUERY_URN, queryUrn);
        return this;
    }

    public EventLoggerEventData queryPosition(int queryPosition) {
        addToPayload(QUERY_POSITION, queryPosition);
        return this;
    }

    public EventLoggerEventData action(String play) {
        addToPayload(ACTION, play);
        return this;
    }

    public EventLoggerEventData reason(String stopReason) {
        addToPayload(PAUSE_REASON, stopReason);
        return this;
    }

    public EventLoggerEventData referrer(String referrer) {
        addToPayload(REFERRER, referrer);
        return this;
    }

    public EventLoggerEventData promotedBy(String promoterUrn) {
        addToPayload(PROMOTED_BY, promoterUrn);
        return this;
    }

    public EventLoggerEventData playheadPosition(long position) {
        addToPayload(PLAYHEAD_POSITION, position);
        return this;
    }

    public EventLoggerEventData inOfflinePlaylist(boolean inOfflinePlaylist) {
        addToPayload(IN_PLAYLIST, inOfflinePlaylist);
        return this;
    }

    public EventLoggerEventData inOfflineLikes(boolean inLikes) {
        addToPayload(IN_LIKES, inLikes);
        return this;
    }

    public EventLoggerEventData eventStage(String eventStage) {
        addToPayload(OFFLINE_EVENT_STAGE, eventStage);
        return this;
    }

    public EventLoggerEventData experiment(String layerTrackingName, int variantId) {
        addToPayload(layerTrackingName, variantId);
        return this;
    }

    @Deprecated // this is added to the base event in v1
    public EventLoggerEventData connectionType(String connectionType) {
        addToPayload(CONNECTION_TYPE, connectionType);
        return this;
    }

    public EventLoggerEventData fromOverflowMenu(boolean fromOverflow) {
        // Not supported by v0
        return this;
    }

    public EventLoggerEventData clickSource(String source) {
        // Not supported by v0
        return this;
    }

    public EventLoggerEventData clickSourceUrn(String sourceUrn) {
        // Not supported by v0
        return this;
    }

    protected void addToPayload(String key, boolean value) {
        addToPayload(key, String.valueOf(value));
    }

    protected void addToPayload(String key, int value) {
        addToPayload(key, String.valueOf(value));
    }

    protected void addToPayload(String key, long value) {
        addToPayload(key, String.valueOf(value));
    }

    protected void addToPayload(String key, String value) {
        if (Strings.isNotBlank(value)) {
            payload.put(key, value);
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventLoggerEventData)) return false;

        EventLoggerEventData that = (EventLoggerEventData) o;

        return MoreObjects.equal(event, that.event)
                && MoreObjects.equal(version, that.version)
                && MoreObjects.equal(payload, that.payload);
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(event, version, payload);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("event", event)
                          .add("version", version)
                          .add("payload", payload).toString();
    }

}
