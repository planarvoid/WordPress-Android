package com.soundcloud.android.testsupport.fixtures;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;

import java.util.ArrayList;
import java.util.List;

public class TestUserRecord implements UserRecord {

    public static List<TestUserRecord> create(int count) {
        List<TestUserRecord> testUserRecords = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            testUserRecords.add(new TestUserRecord());
        }
        return testUserRecords;
    }

    private final ApiUser apiUser;

    public TestUserRecord() {
        apiUser = ModelFixtures.create(ApiUser.class);
    }

    @Override
    public Urn getUrn() {
        return apiUser.getUrn();
    }

    @Override
    public String getUsername() {
        return apiUser.getUsername();
    }

    @Override
    public String getCountry() {
        return apiUser.getCountry();
    }

    @Override
    public int getFollowersCount() {
        return apiUser.getFollowersCount();
    }

    @Override
    public String getAvatarUrl() {
        return apiUser.getAvatarUrl();
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("description");
    }

    @Override
    public Optional<String> getWebsiteUrl() {
        return Optional.of("website-url");
    }

    @Override
    public Optional<String> getWebsiteName() {
        return Optional.of("website-name");
    }

    @Override
    public Optional<String> getDiscogsName() {
        return Optional.of("discogs-name");
    }

    @Override
    public Optional<String> getMyspaceName() {
        return Optional.of("myspace-name");
    }
}
