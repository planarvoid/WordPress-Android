package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ACTION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ADS_RECEIVED;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AD_DELIVERED;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AD_REQUEST_ENDPOINT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AD_REQUEST_ID;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AD_REQUEST_SUCCESS;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.AD_URN;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ANONYMOUS_ID;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.APP_VERSION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ATTRIBUTING_ACTIVITY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.BITRATE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CLICK_ATTRIBUTES;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CLICK_CATEGORY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CLICK_NAME;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CLICK_OBJECT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CLICK_TARGET;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CLIENT_ID;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.COLUMN_COUNT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CONNECTION_TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CONSUMER_SUBS_PLAN;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.CONTEXT_POSITION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.DETAILS;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ERROR_CODE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.EXTERNAL_MEDIA;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.FORMAT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.HOST;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.IMPRESSION_CATEGORY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.IMPRESSION_NAME;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.IMPRESSION_OBJECT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.IN_FOREGROUND;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.IN_LIKES;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.IN_PLAYLIST;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.IN_SYSTEM_PLAYLIST;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.ITEM;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.LATENCY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.LINK_TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.LOCAL_STORAGE_PLAYBACK;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.MEDIA_TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.METRIC_NAME;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.METRIC_VALUE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.MODULE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.MODULE_NAME;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.MODULE_POSITION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.MONETIZATION_MODEL;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.MONETIZATION_TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.MONETIZED_OBJECT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.OFFLINE_EVENT_STAGE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.OS;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.OVERFLOW_MENU;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PAGEVIEW_ID;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PAGE_NAME;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PAGE_URN;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PAUSE_REASON;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PLAYER_INTERFACE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PLAYER_TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PLAYER_VISIBLE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PLAYHEAD_POSITION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PLAYLIST_POSITION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PLAY_ID;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.POLICY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PROMOTED_BY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.PROTOCOL;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.QUERY;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.QUERY_POSITION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.QUERY_URN;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.REFERRER;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.REFERRING_EVENT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.REPEAT;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.REPOSTER;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.RESOURCE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SHARE_LINK_TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SOURCE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SOURCE_QUERY_POSITION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SOURCE_QUERY_URN;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SOURCE_URN;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.SOURCE_VERSION;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.TIMESTAMP;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.TRACK;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.TRACK_LENGTH;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.TRACK_OWNER;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.TRIGGER;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.TYPE;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.URL;
import static com.soundcloud.android.analytics.eventlogger.EventLoggerParam.USER;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.ScrollDepthEvent.ItemDetails;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class EventLoggerEventData {
    private static final String PAGEVIEW_EVENT = "pageview";
    private static final String FOREGROUND_EVENT = "foreground";
    static final String ITEM_INTERACTION = "item_interaction";

    @JsonProperty("event") final String event;
    @JsonProperty("version") private final String version;
    @JsonProperty("payload") final HashMap<String, Object> payload;

    public EventLoggerEventData(String event,
                                String version,
                                int clientId,
                                String anonymousId,
                                String loggedInUserUrn,
                                long timestamp,
                                String connectionType,
                                String appVersion) {
        this.event = event;
        this.version = version;
        this.payload = new HashMap<>();
        addToPayload(CLIENT_ID, clientId);
        addToPayload(ANONYMOUS_ID, anonymousId);
        addToPayload(USER, loggedInUserUrn);
        addToPayload(TIMESTAMP, timestamp);
        addToPayload(CONNECTION_TYPE, connectionType);
        addToPayload(APP_VERSION, appVersion);
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

    public EventLoggerEventData adUrn(Optional<Urn> adUrn) {
        if (adUrn.isPresent()) {
            addToPayload(AD_URN, adUrn.get().toString());
        }

        return this;
    }

    public EventLoggerEventData adDelivered(String adUrn) {
        addToPayload(AD_DELIVERED, adUrn);
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

    public EventLoggerEventData externalMedia(Optional<Uri> externalMedia) {
        if (externalMedia.isPresent()) {
            addToPayload(EXTERNAL_MEDIA, externalMedia.get().toString());
        }

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

    public EventLoggerEventData contextPosition(int position) {
        addToPayload(CONTEXT_POSITION, position);
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

    public EventLoggerEventData adsReceived(String adsReceived) {
        addToPayload(ADS_RECEIVED, adsReceived);
        return this;
    }

    public EventLoggerEventData adRequestId(String uuid) {
        addToPayload(AD_REQUEST_ID, uuid);
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

    public EventLoggerEventData pageviewId(String uuid) {
        addToPayload(PAGEVIEW_ID, uuid);
        return this;
    }

    public EventLoggerEventData attributingActivity(String attributingActivityType, String attributingActivityResource) {
        final HashMap<String, String> attributingActivity = new HashMap<>();

        attributingActivity.put(EventLoggerParam.ACTIVITY_TYPE, attributingActivityType);
        attributingActivity.put(RESOURCE, attributingActivityResource);

        addToPayload(ATTRIBUTING_ACTIVITY, attributingActivity);

        return this;
    }

    public EventLoggerEventData linkType(String linkType) {
        addToPayload(LINK_TYPE, linkType);

        return this;
    }

    public EventLoggerEventData item(String item) {
        addToPayload(ITEM, item);

        return this;
    }

    public EventLoggerEventData module(String name) {
        final HashMap<String, String> module = new HashMap<>();

        module.put(MODULE_NAME, name);

        addToPayload(MODULE, module);

        return this;
    }

    public EventLoggerEventData modulePosition(int position) {
        addToPayload(MODULE_POSITION, position);

        return this;
    }

    public EventLoggerEventData playerInterface(String playerInterface) {
        addToPayload(PLAYER_INTERFACE, playerInterface);
        return this;
    }

    EventLoggerEventData referringEvent(Optional<ReferringEvent> referringEvent) {
        if (referringEvent.isPresent()) {
            final HashMap<String, String> referringEventMap = new HashMap<>(2);

            referringEventMap.put(EventLoggerParam.UUID, referringEvent.get().getId());
            referringEventMap.put(EventLoggerParam.REFERRING_EVENT_KIND, referringEvent.get().getKind());

            addToPayload(REFERRING_EVENT, referringEventMap);
        }
        return this;
    }

    EventLoggerEventData referringEvent(String uuid, String kind) {
        final HashMap<String, String> referringEvent = new HashMap<>();

        referringEvent.put(EventLoggerParam.UUID, uuid);
        referringEvent.put(EventLoggerParam.REFERRING_EVENT_KIND, toReferringEventName(kind));

        addToPayload(REFERRING_EVENT, referringEvent);

        return this;
    }

    public EventLoggerEventData playerType(String playa) {
        addToPayload(PLAYER_TYPE, playa);
        return this;
    }

    public EventLoggerEventData clientEventId(String clientEventId) {
        addToPayload(EventLoggerParam.CLIENT_EVENT_ID, clientEventId);
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

    public EventLoggerEventData inSystemPlaylist(Urn systemPlaylistUrn) {
        addToPayload(IN_SYSTEM_PLAYLIST, String.valueOf(systemPlaylistUrn));
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
        addToPayload(LATENCY, latency);
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

    public EventLoggerEventData bitrate(int bitrate) {
        addToPayload(BITRATE, bitrate);
        return this;
    }

    public EventLoggerEventData format(String format) {
        addToPayload(FORMAT, format.toLowerCase(Locale.US));
        return this;
    }

    public EventLoggerEventData playbackDetails(Optional<String> json) {
        if (json.isPresent()) {
            addJsonToPayload(DETAILS, json.get());
        }
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

    public EventLoggerEventData action(String action) {
        addToPayload(ACTION, action);
        return this;
    }

    public EventLoggerEventData playId(String playId) {
        addToPayload(PLAY_ID, playId);
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

    public EventLoggerEventData experiment(String key, String value) {
        addToPayload(key, value);
        return this;
    }

    public EventLoggerEventData columnCount(int columnCount) {
        addToPayload(COLUMN_COUNT, columnCount);
        return this;
    }

    EventLoggerEventData itemDetails(String key, ItemDetails itemDetails) {
        final HashMap<String, Object> details = new HashMap<>(3);

        details.put(EventLoggerParam.COLUMN, itemDetails.column());
        details.put(EventLoggerParam.POSITION, itemDetails.position());
        details.put(EventLoggerParam.VIEWABLE_PERCENT, itemDetails.viewablePercentage());

        addToPayload(key, details);
        return this;
    }

    public EventLoggerEventData fromOverflowMenu(boolean fromOverflowMenu) {
        getClickAttributes().put(OVERFLOW_MENU, fromOverflowMenu);
        return this;
    }

    public EventLoggerEventData clickSource(String source) {
        if (Strings.isNotBlank(source)) {
            getClickAttributes().put(SOURCE, source);
        }
        return this;
    }

    public EventLoggerEventData clickSourceQueryUrn(String queryUrn) {
        getClickAttributes().put(SOURCE_QUERY_URN, queryUrn);
        return this;
    }

    public EventLoggerEventData clickSourceQueryPosition(Integer clickSourcePosition) {
        getClickAttributes().put(SOURCE_QUERY_POSITION, clickSourcePosition);
        return this;
    }

    public EventLoggerEventData clickSourceUrn(String sourceUrn) {
        if (Strings.isNotBlank(sourceUrn)) {
            getClickAttributes().put(SOURCE_URN, sourceUrn);
        }
        return this;
    }

    public EventLoggerEventData clickTrigger(String trigger) {
        if (Strings.isNotBlank(trigger)) {
            getClickAttributes().put(TRIGGER, trigger);
        }
        return this;
    }

    public EventLoggerEventData clickRepeat(String repeatMode) {
        if (Strings.isNotBlank(repeatMode)) {
            getClickAttributes().put(REPEAT, repeatMode);
        }
        return this;
    }

    public EventLoggerEventData searchQuery(String query) {
        if (Strings.isNotBlank(query)) {
            getClickAttributes().put(QUERY, query);
        }
        return this;
    }

    public EventLoggerEventData shareLinkType(String shareLinkType) {
        if (Strings.isNotBlank(shareLinkType)) {
            getClickAttributes().put(SHARE_LINK_TYPE, shareLinkType);
        }
        return this;
    }

    protected void addToPayload(String key, boolean value) {
        payload.put(key, value);
    }

    protected void addToPayload(String key, int value) {
        payload.put(key, value);
    }

    protected void addToPayload(String key, long value) {
        payload.put(key, value);
    }

    protected void addToPayload(String key, Map<String, ?> child) {
        if (child.size() > 0) {
            payload.put(key, child);
        }
    }

    protected void addToPayload(String key, String value) {
        if (Strings.isNotBlank(value)) {
            payload.put(key, value);
        }
    }

    private void addJsonToPayload(String key, String value) {
        if (Strings.isNotBlank(value)) {
            try {
                payload.put(key, JacksonJsonTransformer.buildObjectMapper().readValue(value, JsonNode.class));
            } catch (IOException e) {
                // ignore malformed json
            }
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

    @VisibleForTesting
    String toReferringEventName(String eventKind) {
        if (eventKind.equals(UIEvent.Kind.NAVIGATION.toString())) {
            return ITEM_INTERACTION;
        }
        switch (eventKind) {
            case ScreenEvent.KIND:
                return PAGEVIEW_EVENT;
            case ForegroundEvent.KIND_OPEN:
                return FOREGROUND_EVENT;
            default:
                throw new IllegalArgumentException(
                        "Unable to transform from event kind to event logger event name. Unknown event kind: " + eventKind);
        }
    }

    private HashMap<String, Object> getClickAttributes() {
        if (!payload.containsKey(CLICK_ATTRIBUTES)) {
            payload.put(CLICK_ATTRIBUTES, new HashMap<String, Object>());
        }
        return (HashMap<String, Object>) payload.get(CLICK_ATTRIBUTES);
    }
}
