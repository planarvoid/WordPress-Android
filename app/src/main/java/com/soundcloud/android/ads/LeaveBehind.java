package com.soundcloud.android.ads;

public class LeaveBehind {

    private final String imageUrl;
    private final String linkUrl;

    public LeaveBehind(String imageUrl, String linkUrl) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

}
