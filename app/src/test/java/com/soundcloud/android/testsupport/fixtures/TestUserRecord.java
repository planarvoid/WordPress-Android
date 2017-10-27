package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Date;
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
        apiUser = UserFixtures.apiUser();
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
    public Optional<String> getFirstName() {
        return apiUser.getFirstName();
    }

    @Override
    public Optional<String> getLastName() {
        return apiUser.getLastName();
    }

    @Override
    public Optional<Date> getCreatedAt() {
        return apiUser.getCreatedAt();
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
    public int getFollowingsCount() {
        return apiUser.getFollowingsCount();
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("description");
    }

    @Override
    public Optional<Urn> getArtistStationUrn() {
        return Optional.of(Urn.forArtistStation(123));
    }

    @Override
    public Optional<String> getVisualUrlTemplate() {
        return apiUser.getVisualUrlTemplate();
    }

    @Override
    public boolean isPro() {
        return apiUser.isPro();
    }
}
