package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.SocialEvent;

import java.util.Map;

class LocalyticsSocialEventHandler {
    private LocalyticsSession mLocalyticsSession;

    LocalyticsSocialEventHandler(LocalyticsSession localyticsSession) {
        mLocalyticsSession = localyticsSession;
    }

    public void handleEvent(SocialEvent sourceEvent) {
        handleEvent(sourceEvent.getKind(), sourceEvent.getAttributes());
    }

    private void handleEvent(int sourceEventType, Map<String, String> eventAttributes) {
        switch (sourceEventType) {
            case SocialEvent.FOLLOW:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.FOLLOW, eventAttributes);
                break;
            case SocialEvent.UNFOLLOW:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.UNFOLLOW, eventAttributes);
                break;
            case SocialEvent.LIKE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.LIKE, eventAttributes);
                break;
            case SocialEvent.UNLIKE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.UNLIKE, eventAttributes);
                break;
            case SocialEvent.REPOST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.REPOST, eventAttributes);
                break;
            case SocialEvent.UNREPOST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.UNREPOST, eventAttributes);
                break;
            case SocialEvent.ADD_TO_PLAYLIST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.ADD_TO_PLAYLIST, eventAttributes);
                break;
            case SocialEvent.COMMENT:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.COMMENT, eventAttributes);
                break;
            case SocialEvent.SHARE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.SHARE, eventAttributes);
                break;
            default:
                throw new IllegalArgumentException("Social Event type is invalid");
        }
    }

}
