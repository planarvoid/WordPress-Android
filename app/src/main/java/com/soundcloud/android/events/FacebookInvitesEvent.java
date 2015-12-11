package com.soundcloud.android.events;

import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.main.Screen;
import org.jetbrains.annotations.NotNull;

public final class FacebookInvitesEvent extends TrackingEvent {

    public static final String KIND_IMPRESSION = "impression";
    public static final String KIND_CLICK = "click";

    public static final String KEY_PAGE_NAME = "page_name";
    public static final String KEY_IMPRESSION_CATEGORY = "impression_category";
    public static final String KEY_IMPRESSION_NAME = "impression_name";
    public static final String KEY_CLICK_CATEGORY = "click_category";
    public static final String KEY_CLICK_NAME = "click_name";

    public static final String CATEGORY_INVITE_FRIENDS = "invite_friends";
    public static final String TYPE_LISTENER_WITH_IMAGES = "fb::with_images";
    public static final String TYPE_LISTENER_WITHOUT_IMAGES = "fb::no_image";
    public static final String TYPE_LISTENER_DISMISS_WITH_IMAGES = "fb::with_images::dismiss";
    public static final String TYPE_LISTENER_DISMISS_WITHOUT_IMAGES = "fb::no_image::dismiss";
    public static final String TYPE_CREATOR_WITH_IMAGES = "fb::creator_with_images";
    public static final String TYPE_CREATOR_DISMISS_WITH_IMAGES = "fb::creator_with_images::dismiss";

    private FacebookInvitesEvent(@NotNull String kind, long timeStamp) {
        super(kind, timeStamp);
    }

    public static FacebookInvitesEvent forListenerShown(FacebookInvitesItem item) {
        return baseImpressionEvent(withListenerImages(item));
    }

    public static TrackingEvent forCreatorShown() {
        return baseImpressionEvent(TYPE_CREATOR_WITH_IMAGES);
    }

    public static FacebookInvitesEvent forListenerClick(FacebookInvitesItem item) {
        return baseClickEvent(withListenerImages(item));
    }

    public static FacebookInvitesEvent forCreatorClick() {
        return baseClickEvent(TYPE_CREATOR_WITH_IMAGES);
    }

    public static FacebookInvitesEvent forListenerDismiss(FacebookInvitesItem item) {
        return baseClickEvent(dismissWithListenerImages(item));
    }

    public static FacebookInvitesEvent forCreatorDismiss() {
        return baseClickEvent(TYPE_CREATOR_DISMISS_WITH_IMAGES);
    }

    private static FacebookInvitesEvent baseEvent(String kind, String screen) {
        return new FacebookInvitesEvent(kind, System.currentTimeMillis())
                .put(KEY_PAGE_NAME, screen);
    }

    private static FacebookInvitesEvent baseClickEvent(String clickName) {
        return baseEvent(KIND_CLICK, Screen.STREAM.get())
                .put(KEY_CLICK_CATEGORY, CATEGORY_INVITE_FRIENDS)
                .put(KEY_CLICK_NAME, clickName);
    }

    private static FacebookInvitesEvent baseImpressionEvent(String impressionName) {
        return baseEvent(KIND_IMPRESSION, Screen.STREAM.get())
                .put(KEY_IMPRESSION_CATEGORY, CATEGORY_INVITE_FRIENDS)
                .put(KEY_IMPRESSION_NAME, impressionName);
    }

    private static String withListenerImages(FacebookInvitesItem item) {
        return item.hasPictures() ? TYPE_LISTENER_WITH_IMAGES : TYPE_LISTENER_WITHOUT_IMAGES;
    }

    private static String dismissWithListenerImages(FacebookInvitesItem item) {
        return item.hasPictures() ? TYPE_LISTENER_DISMISS_WITH_IMAGES : TYPE_LISTENER_DISMISS_WITHOUT_IMAGES;
    }
}
