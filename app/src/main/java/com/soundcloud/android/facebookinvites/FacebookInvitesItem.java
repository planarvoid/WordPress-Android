package com.soundcloud.android.facebookinvites;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.NotificationItem;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

public class FacebookInvitesItem extends NotificationItem {
    public static final Urn URN = new Urn("soundcloud:notifications:facebook-invites");
    private final List<String> friendPictureUrls;

    public FacebookInvitesItem(List<String> friendPictureUrls) {
        this.friendPictureUrls = friendPictureUrls;
    }

    @Override
    public Urn getEntityUrn() {
        return URN;
    }

    public List<String> getFacebookFriendPictureUrls() {
        return friendPictureUrls;
    }

    public boolean hasPictures() {
        return friendPictureUrls.size() > 0;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FacebookInvitesItem)) return false;

        FacebookInvitesItem that = (FacebookInvitesItem) o;
        return MoreObjects.equal(friendPictureUrls, that.friendPictureUrls);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(friendPictureUrls);
    }
}
