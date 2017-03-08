package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.strings.Strings;

public abstract class AdData {

    public enum MonetizationType {
        PROMOTED("promoted"),
        INTERSTITIAL("interstitial"),
        LEAVE_BEHIND("leave_behind"),
        AUDIO("audio_ad"),
        VIDEO("video_ad"),
        INLAY("mobile_inlay");

        private final String key;

        MonetizationType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private Urn monetizableTrackUrn;
    private String monetizableTitle = Strings.EMPTY;
    private String monetizableCreator = Strings.EMPTY;

    public abstract Urn getAdUrn();

    public Urn getMonetizableTrackUrn() {
        return monetizableTrackUrn;
    }

    public String getMonetizableTitle() {
        return monetizableTitle;
    }

    public String getMonetizableCreator() {
        return monetizableCreator;
    }

    public void setMonetizableTitle(String trackTitle) {
        monetizableTitle = trackTitle;
    }

    public void setMonetizableCreator(String trackCreator) {
        monetizableCreator = trackCreator;
    }

    public boolean hasMonetizableTitleAndCreator() {
        return !Strings.isNullOrEmpty(getMonetizableTitle()) && !Strings.isNullOrEmpty(getMonetizableCreator());
    }

    protected void setMonetizableTrackUrn(Urn trackUrn) {
        monetizableTrackUrn = trackUrn;
    }

    public abstract MonetizationType getMonetizationType();
}
