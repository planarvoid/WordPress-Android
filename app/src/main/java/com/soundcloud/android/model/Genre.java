package com.soundcloud.android.model;

import com.google.common.base.Objects;

public class Genre extends ScModel {

    public enum Grouping { FACEBOOK_LIKES, FACEBOOK_FRIENDS, MUSIC, AUDIO }

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Genre)) {
            return false;
        }
        Genre that = (Genre) o;
        return Objects.equal(mPermalink, that.mPermalink);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPermalink);
    }
}
