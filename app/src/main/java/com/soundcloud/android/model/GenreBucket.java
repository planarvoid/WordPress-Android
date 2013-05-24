package com.soundcloud.android.model;

import com.google.common.base.Objects;

import java.util.Collections;
import java.util.List;

public class GenreBucket {

    private Genre mGenre;
    private List<User> mUsers;

    public GenreBucket(Genre genre) {
        mGenre = genre;
        mUsers = Collections.emptyList();
    }

    public Genre getGenre() {
        return mGenre;
    }

    public void setGenre(Genre genre) {
        mGenre = genre;
    }

    public List<User> getUsers() {
        return mUsers;
    }

    public void setUsers(List<User> users) {
        mUsers = users;
    }

    public boolean hasUsers() {
        return !mUsers.isEmpty();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("genre", mGenre.getName()).add("users count", mUsers.size()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GenreBucket)) {
            return false;
        }
        GenreBucket that = (GenreBucket) o;
        return Objects.equal(mGenre, that.getGenre()) && Objects.equal(mUsers, that.getUsers());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mGenre, mUsers);
    }
}
