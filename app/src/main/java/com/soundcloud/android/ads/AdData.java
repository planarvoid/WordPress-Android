package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;

public abstract class AdData {
    private Urn monetizableTrackUrn;
    private String monetizableTitle;
    private String monetizableCreator;

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

    protected void setMonetizableTrackUrn(Urn trackUrn) {
       monetizableTrackUrn = trackUrn;
    }
}
