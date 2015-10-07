package com.soundcloud.android.events;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import org.jetbrains.annotations.NotNull;

public final class StreamNotificationEvent extends TrackingEvent {

    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_IMPRESSION_CATEGORY = "impression_category";
    public static final String KEY_IMPRESSION_NAME = "impression_name";
    public static final String KEY_CLICK_CATEGORY = "click_category";
    public static final String KEY_CLICK_NAME = "click_name";

    public static final String TYPE_INVITE_FRIENDS = "invite_friends";
    public static final String TYPE_WITH_IMAGES = "fb::with_images";
    public static final String TYPE_WITHOUT_IMAGES = "fb::no_image";
    public static final String TYPE_DISMISS_WITH_IMAGES = "fb::with_images::dismiss";
    public static final String TYPE_DISMISS_WITHOUT_IMAGES = "fb::no_image::dismiss";

    private StreamNotificationEvent(@NotNull String kind, long timeStamp) {
        super(kind, timeStamp);
    }

    public static StreamNotificationEvent forFacebookInviteShown(FacebookInvitesItem item) {
        StreamNotificationEvent event = baseEvent(KIND_IMPRESSION, Screen.STREAM.get());
        event.put(KEY_IMPRESSION_CATEGORY, TYPE_INVITE_FRIENDS);
        event.put(KEY_IMPRESSION_NAME, withImages(item));
        return event;
    }

    public static StreamNotificationEvent forFacebookInviteClick(FacebookInvitesItem item) {
        return baseEvent(KIND_CLICK, Screen.STREAM.get())
                .put(KEY_CLICK_CATEGORY, TYPE_INVITE_FRIENDS)
                .put(KEY_CLICK_NAME, withImages(item));
    }

    public static StreamNotificationEvent forFacebookInviteDismissed(FacebookInvitesItem item) {
        return baseEvent(KIND_CLICK, Screen.STREAM.get())
                .put(KEY_CLICK_CATEGORY, TYPE_INVITE_FRIENDS)
                .put(KEY_CLICK_NAME, dismissWithImages(item));
    }

    private static StreamNotificationEvent baseEvent(String kind, String screen) {
        return new StreamNotificationEvent(kind, System.currentTimeMillis())
                .put(KEY_PAGE_NAME, screen);
    }

    private static String withImages(FacebookInvitesItem item) {
        return item.hasPictures() ? TYPE_WITH_IMAGES : TYPE_WITHOUT_IMAGES;
    }

    private static String dismissWithImages(FacebookInvitesItem item) {
        return item.hasPictures() ? TYPE_DISMISS_WITH_IMAGES : TYPE_DISMISS_WITHOUT_IMAGES;
    }
}
