package com.soundcloud.android.facebookinvites;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.NotificationItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.List;

public class FacebookInvitesItem extends NotificationItem {
    public static final Urn LISTENER_URN = new Urn("soundcloud:notifications:facebook-listener-invites");
    public static final Urn CREATOR_URN = new Urn("soundcloud:notifications:facebook-creator-invites");

    private final Urn urn;
    private Optional<List<String>> friendPictureUrls = Optional.absent();
    private Urn trackUrn = Urn.NOT_SET;
    private String trackUrl;

    public FacebookInvitesItem(Urn urn) {
        this.urn = urn;
    }

    public FacebookInvitesItem(Urn urn, PropertySet track) {
        this.urn = urn;
        this.trackUrn = track.get(PlayableProperty.URN);
        this.trackUrl = track.get(PlayableProperty.PERMALINK_URL);
    }

    @Override
    public Urn getEntityUrn() {
        return urn;
    }

    public boolean hasPictures() {
        return friendPictureUrls.isPresent() && !friendPictureUrls.get().isEmpty();
    }

    void setFacebookFriendPictureUrls(List<String> friendPictureUrls) {
        this.friendPictureUrls = Optional.of(friendPictureUrls);
    }

    Optional<List<String>> getFacebookFriendPictureUrls() {
        return friendPictureUrls;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public String getTrackUrl() {
        return trackUrl;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FacebookInvitesItem)) return false;

        FacebookInvitesItem that = (FacebookInvitesItem) o;
        return MoreObjects.equal(urn, that.urn)
                && MoreObjects.equal(friendPictureUrls, that.friendPictureUrls)
                && MoreObjects.equal(trackUrn, that.trackUrn)
                && MoreObjects.equal(trackUrl, that.trackUrl);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(urn, friendPictureUrls, trackUrn, trackUrl);
    }
}
