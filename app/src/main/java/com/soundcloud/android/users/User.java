package com.soundcloud.android.users;

import static com.soundcloud.android.model.Urn.STRING_TO_URN;
import static com.soundcloud.android.model.Urn.forUser;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.Users;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;

import java.util.Date;

@AutoValue
public abstract class User {

    public abstract Urn urn();

    public abstract String username();

    public abstract Optional<String> firstName();

    public abstract Optional<String> lastName();

    public abstract Optional<Date> signupDate();

    public abstract Optional<String> country();

    public abstract Optional<String> city();

    public abstract int followersCount();

    public abstract int followingsCount();

    public abstract Optional<String> description();

    public abstract Optional<String> avatarUrl();

    public abstract Optional<String> visualUrl();

    public abstract Optional<String> websiteUrl();

    public abstract Optional<String> websiteName();

    public abstract Optional<String> mySpaceName();

    public abstract Optional<String> discogsName();

    public abstract Optional<Urn> artistStation();

    public abstract boolean isPro();

    public abstract User.Builder toBuilder();

    public static User fromCursorReader(CursorReader cursorReader) {
        return new AutoValue_User.Builder()
                .urn(forUser(cursorReader.getLong(Users._ID.name())))
                .username(cursorReader.getString(Users.USERNAME.name()))
                .firstName(Optional.fromNullable(cursorReader.getString(Users.FIRST_NAME.name())))
                .lastName(Optional.fromNullable(cursorReader.getString(Users.LAST_NAME.name())))
                .signupDate(cursorReader.hasColumn(Users.SIGNUP_DATE.name()) ? Optional.of(cursorReader.getDateFromTimestamp(Users.SIGNUP_DATE.name())) : Optional.absent())
                .country(Optional.fromNullable(cursorReader.getString(Users.COUNTRY.name())))
                .city(Optional.fromNullable(cursorReader.getString(Users.CITY.name())))
                .followersCount(cursorReader.getInt(Users.FOLLOWERS_COUNT.name()))
                .followingsCount(cursorReader.getInt(Users.FOLLOWINGS_COUNT.name()))
                .description(Optional.fromNullable(cursorReader.getString(Users.DESCRIPTION.name())))
                .avatarUrl(Optional.fromNullable(cursorReader.getString(Users.AVATAR_URL.name())))
                .visualUrl(Optional.fromNullable(cursorReader.getString(Users.VISUAL_URL.name())))
                .websiteUrl(Optional.fromNullable(cursorReader.getString(Users.WEBSITE_URL.name())))
                .websiteName(Optional.fromNullable(cursorReader.getString(Users.WEBSITE_NAME.name())))
                .mySpaceName(Optional.fromNullable(cursorReader.getString(Users.MYSPACE_NAME.name())))
                .discogsName(Optional.fromNullable(cursorReader.getString(Users.DISCOGS_NAME.name())))
                .artistStation(Optional.fromNullable(cursorReader.getString(Users.ARTIST_STATION.name())).transform(STRING_TO_URN))
                .isPro(cursorReader.getBoolean(Users.IS_PRO.name()))
                .build();
    }

    public static User fromApiUser(ApiUser apiUser) {
        return new AutoValue_User.Builder()
                .urn(apiUser.getUrn())
                .username(apiUser.getUsername())
                .firstName(apiUser.getFirstName())
                .lastName(apiUser.getLastName())
                .signupDate(apiUser.getCreatedAt())
                .country(Optional.fromNullable(apiUser.getCountry()))
                .city(Optional.fromNullable(apiUser.getCity()))
                .followersCount(apiUser.getFollowersCount())
                .followingsCount(apiUser.getFollowingsCount())
                .description(apiUser.getDescription())
                .avatarUrl(apiUser.getImageUrlTemplate())
                .visualUrl(apiUser.getVisualUrlTemplate())
                .websiteUrl(apiUser.getWebsiteUrl())
                .websiteName(apiUser.getWebsiteName())
                .mySpaceName(apiUser.getMyspaceName())
                .discogsName(apiUser.getDiscogsName())
                .artistStation(apiUser.getArtistStationUrn())
                .isPro(apiUser.isPro())
                .build();
    }

    public static Builder builder() {
        // we only do this because we cannot extend the automatic optional functionality
        // of autovalue to include our optional : https://github.com/google/auto/issues/359
        return new AutoValue_User.Builder()
                .country(Optional.absent())
                .firstName(Optional.absent())
                .lastName(Optional.absent())
                .signupDate(Optional.absent())
                .city(Optional.absent())
                .description(Optional.absent())
                .avatarUrl(Optional.absent())
                .visualUrl(Optional.absent())
                .websiteUrl(Optional.absent())
                .websiteName(Optional.absent())
                .mySpaceName(Optional.absent())
                .discogsName(Optional.absent())
                .artistStation(Optional.absent());

    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder urn(Urn urn);

        public abstract Builder username(String username);

        public abstract Builder firstName(Optional<String> firstName);

        public abstract Builder lastName(Optional<String> lastName);

        public abstract Builder signupDate(Optional<Date> signupDate);

        public abstract Builder country(Optional<String> country);

        public abstract Builder city(Optional<String> city);

        public abstract Builder followersCount(int followerCount);

        public abstract Builder followingsCount(int followingsCount);

        public abstract Builder description(Optional<String> description);

        public abstract Builder avatarUrl(Optional<String> avatarUrl);

        public abstract Builder visualUrl(Optional<String> visualUrl);

        public abstract Builder websiteUrl(Optional<String> websiteUrl);

        public abstract Builder websiteName(Optional<String> websiteName);

        public abstract Builder mySpaceName(Optional<String> myspaceName);

        public abstract Builder discogsName(Optional<String> discogsName);

        public abstract Builder artistStation(Optional<Urn> artistStation);

        public abstract Builder isPro(boolean isPro);

        public abstract User build();
    }
}
