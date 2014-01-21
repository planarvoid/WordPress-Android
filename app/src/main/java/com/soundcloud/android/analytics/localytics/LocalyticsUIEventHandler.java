package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.UIEvent;

import java.util.Map;

class LocalyticsUIEventHandler {

    private LocalyticsSession mLocalyticsSession;

    LocalyticsUIEventHandler(LocalyticsSession localyticsSession) {
        mLocalyticsSession = localyticsSession;
    }

    public void handleEvent(UIEvent sourceEvent) {
        handleEvent(sourceEvent.getKind(), sourceEvent.getAttributes());
    }

    private void handleEvent(int sourceEventType, Map<String, String> eventAttributes) {
        switch (sourceEventType) {
            case UIEvent.FOLLOW:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.FOLLOW, eventAttributes);
                break;
            case UIEvent.UNFOLLOW:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.UNFOLLOW, eventAttributes);
                break;
            case UIEvent.LIKE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.LIKE, eventAttributes);
                break;
            case UIEvent.UNLIKE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.UNLIKE, eventAttributes);
                break;
            case UIEvent.REPOST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.REPOST, eventAttributes);
                break;
            case UIEvent.UNREPOST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.UNREPOST, eventAttributes);
                break;
            case UIEvent.ADD_TO_PLAYLIST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.ADD_TO_PLAYLIST, eventAttributes);
                break;
            case UIEvent.COMMENT:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.COMMENT, eventAttributes);
                break;
            case UIEvent.SHARE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.SHARE, eventAttributes);
                break;
            case UIEvent.SHUFFLE_LIKES:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.SHUFFLE_LIKES, eventAttributes);
                break;
            case UIEvent.NAV_PROFILE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.NAV_PROFILE, eventAttributes);
                break;
            case UIEvent.NAV_STREAM:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.NAV_STREAM, eventAttributes);
                break;
            case UIEvent.NAV_EXPLORE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.NAV_EXPLORE, eventAttributes);
                break;
            case UIEvent.NAV_LIKES:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.NAV_LIKES, eventAttributes);
                break;
            case UIEvent.NAV_PLAYLISTS:
                mLocalyticsSession.tagEvent(LocalyticsEvents.UI.NAV_PLAYLISTS, eventAttributes);
                break;
            default:
                throw new IllegalArgumentException("UI Event type is invalid");
        }
    }

}
