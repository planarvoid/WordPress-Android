package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;

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
    public Optional<String> getImageUrlTemplate() {
        return apiUser.getImageUrlTemplate();
    }

    @Override
    public String getPermalink() {
        return apiUser.getPermalink();
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
    public String getCity() {
        return apiUser.getCity();
    }

    @Override
    public int getFollowersCount() {
        return apiUser.getFollowersCount();
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

    @Override
    public Optional<Urn> getArtistStationUrn() {
        return Optional.of(Urn.forArtistStation(123));
    }

    @Override
    public Optional<String> getVisualUrlTemplate() {
        return apiUser.getVisualUrlTemplate();
    }
}
