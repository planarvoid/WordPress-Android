package com.soundcloud.android.model;

public class Genre extends ScModel {

    public enum Grouping { MUSIC, AUDIO }

    private String mName, mPermalink;
    private Grouping mGrouping;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public Grouping getGrouping() {
        return mGrouping;
    }

    public void setGrouping(String grouping) {
        this.mGrouping = Grouping.valueOf(grouping);
    }

    public void setGrouping(Grouping grouping) {
        this.mGrouping = grouping;
    }

    public String getPermalink() {
        return mPermalink;
    }

    public void setPermalink(String permalink) {
        this.mPermalink = permalink;
    }
}
