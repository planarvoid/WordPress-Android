package com.soundcloud.android.events;

import static com.soundcloud.android.events.FacebookInvitesEvent.InteractionName.TYPE_CREATOR_DISMISS_WITH_IMAGES;
import static com.soundcloud.android.events.FacebookInvitesEvent.InteractionName.TYPE_CREATOR_WITH_IMAGES;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.main.Screen;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class FacebookInvitesEvent extends NewTrackingEvent {

    private static final String CATEGORY_INVITE_FRIENDS = "invite_friends";

    public enum EventName {
        IMPRESSION("impression"), CLICK("click");
        private final String key;

        EventName(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public enum InteractionName {
        TYPE_LISTENER_WITH_IMAGES("fb::with_images"),
        TYPE_LISTENER_WITHOUT_IMAGES("fb::no_image"),
        TYPE_LISTENER_DISMISS_WITH_IMAGES("fb::with_images::dismiss"),
        TYPE_LISTENER_DISMISS_WITHOUT_IMAGES("fb::no_image::dismiss"),
        TYPE_CREATOR_WITH_IMAGES("fb::creator_with_images"),
        TYPE_CREATOR_DISMISS_WITH_IMAGES("fb::creator_with_images::dismiss");
        private final String key;

        InteractionName(String key) {
            this.key = key;
        }

        public String toString() {
            return key;
        }
    }

    public abstract EventName eventName();

    public abstract String pageName();

    public abstract Optional<String> clickCategory();

    public abstract Optional<String> impressionCategory();

    public abstract Optional<InteractionName> clickName();

    public abstract Optional<InteractionName> impressionName();

    public static FacebookInvitesEvent forListenerShown(boolean hasPictures) {
        return baseImpressionEvent(withListenerImages(hasPictures));
    }

    public static TrackingEvent forCreatorShown() {
        return baseImpressionEvent(TYPE_CREATOR_WITH_IMAGES);
    }

    public static FacebookInvitesEvent forListenerClick(boolean hasPictures) {
        return baseClickEvent(withListenerImages(hasPictures));
    }

    public static FacebookInvitesEvent forCreatorClick() {
        return baseClickEvent(TYPE_CREATOR_WITH_IMAGES);
    }

    public static FacebookInvitesEvent forListenerDismiss(boolean hasPictures) {
        return baseClickEvent(dismissWithListenerImages(hasPictures));
    }

    public static FacebookInvitesEvent forCreatorDismiss() {
        return baseClickEvent(TYPE_CREATOR_DISMISS_WITH_IMAGES);
    }

    @Override
    public TrackingEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_FacebookInvitesEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    private static FacebookInvitesEvent.Builder baseEvent(String screen, EventName eventName) {
        return new AutoValue_FacebookInvitesEvent.Builder().id(defaultId())
                                                           .timestamp(defaultTimestamp())
                                                           .referringEvent(Optional.absent())
                                                           .pageName(screen)
                                                           .eventName(eventName)
                                                           .clickCategory(Optional.absent())
                                                           .impressionCategory(Optional.absent())
                                                           .clickName(Optional.absent())
                                                           .impressionName(Optional.absent());
    }

    private static FacebookInvitesEvent baseClickEvent(InteractionName clickName) {
        return baseEvent(Screen.STREAM.get(), EventName.CLICK).clickName(Optional.of(clickName)).clickCategory(Optional.of(CATEGORY_INVITE_FRIENDS)).build();
    }

    private static FacebookInvitesEvent baseImpressionEvent(InteractionName impressionName) {
        return baseEvent(Screen.STREAM.get(), EventName.IMPRESSION).impressionName(Optional.of(impressionName)).impressionCategory(Optional.of(CATEGORY_INVITE_FRIENDS)).build();
    }

    private static InteractionName withListenerImages(boolean hasPictures) {
        return hasPictures ? InteractionName.TYPE_LISTENER_WITH_IMAGES : InteractionName.TYPE_LISTENER_WITHOUT_IMAGES;
    }

    private static InteractionName dismissWithListenerImages(boolean hasPictures) {
        return hasPictures ? InteractionName.TYPE_LISTENER_DISMISS_WITH_IMAGES : InteractionName.TYPE_LISTENER_DISMISS_WITHOUT_IMAGES;
    }


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder eventName(EventName eventName);

        public abstract Builder pageName(String pageName);

        public abstract Builder clickCategory(Optional<String> clickCategory);

        public abstract Builder impressionCategory(Optional<String> impressionCategory);

        public abstract Builder clickName(Optional<InteractionName> clickName);

        public abstract Builder impressionName(Optional<InteractionName> impressionName);

        public abstract FacebookInvitesEvent build();
    }
}
