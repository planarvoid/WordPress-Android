package com.soundcloud.android.settings.notifications;

import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import java.util.Collection;
import java.util.EnumSet;

enum NotificationPreferenceType {
    //-------------------[SETTING KEY]------[MOBILE KEY]-----------------[MAIL KEY]------------
    MESSAGES(           "messages",         null,                       "messages_mail"),
    GROUPS(             "groups",           null,                       "groups_mail"),
    NEWSLETTERS(        "newsletters",      null,                       "newsletters_mail"),
    COMMENTS(           "comments",         null,                       "comments_mail"),
    FOLLOWS(            "follows",          "follows_mobile",           "follows_mail"),
    NEW_CONTENT(        "newContent",       "newContent_mobile",        "newContent_mail"),
    LIKES(              "likes",            "likes_mobile",             "likes_mail"),
    REPOSTS(            "reposts",          "reposts_mobile",           "reposts_mail"),
    PRODUCT_UPDATES(    "productUpdates",   "productUpdates_mobile",    "productUpdates_mail"),
    SURVEYS(            "surveys",          "surveys_mobile",           "surveys_mail"),
    TIPS(               "tips",             "tips_mobile",              "tips_mail");

    private static EnumSet<NotificationPreferenceType> MOBILE_PREFERENCES =
            EnumSet.of(FOLLOWS, NEW_CONTENT, LIKES, REPOSTS, PRODUCT_UPDATES, SURVEYS, TIPS);

    private static EnumSet<NotificationPreferenceType> MAIL_PREFERENCES =
            EnumSet.of(MESSAGES, GROUPS, NEWSLETTERS, COMMENTS, FOLLOWS, NEW_CONTENT, LIKES,
                    REPOSTS, PRODUCT_UPDATES, SURVEYS, TIPS);

    private static final Function<NotificationPreferenceType, String> TO_MOBILE_KEY = new Function<NotificationPreferenceType, String>() {
        public String apply(NotificationPreferenceType type) {
            return type.mobileKey().get();
        }
    };

    private static final Function<NotificationPreferenceType, String> TO_MAIL_KEY = new Function<NotificationPreferenceType, String>() {
        public String apply(NotificationPreferenceType type) {
            return type.mailKey().get();
        }
    };

    private final String name;
    private final Optional<String> mobileKey;
    private final Optional<String> mailKey;

    NotificationPreferenceType(String name, String mobileKey, String mailKey) {
        this.name = name;
        this.mobileKey = Optional.fromNullable(mobileKey);
        this.mailKey = Optional.fromNullable(mailKey);
    }

    Optional<String> mailKey() {
        return mailKey;
    }

    Optional<String> mobileKey() {
        return mobileKey;
    }

    String getName() {
        return name;
    }

    static Optional<NotificationPreferenceType> from(String key) {
        for (NotificationPreferenceType type : values()) {
            if (type.getName().equals(key)) {
                return Optional.of(type);
            }
        }
        return Optional.absent();
    }

    static Collection<String> mobileKeys() {
        return MoreCollections.transform(MOBILE_PREFERENCES, TO_MOBILE_KEY);
    }

    static Collection<String> mailKeys() {
        return MoreCollections.transform(MAIL_PREFERENCES, TO_MAIL_KEY);
    }

}
