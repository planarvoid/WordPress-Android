package com.soundcloud.android.users;

import static com.soundcloud.android.model.Urn.STRING_TO_URN;
import static com.soundcloud.android.model.Urn.forUser;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.UsersView;
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

    public abstract boolean isFollowing();

    public abstract User.Builder toBuilder();

    public static User fromCursorReader(CursorReader cursorReader) {
        return new AutoValue_User.Builder()
                .urn(forUser(cursorReader.getLong(UsersView.ID.name())))
                .username(cursorReader.getString(UsersView.USERNAME.name()))
                .firstName(Optional.fromNullable(cursorReader.getString(UsersView.FIRST_NAME.name())))
                .lastName(Optional.fromNullable(cursorReader.getString(UsersView.LAST_NAME.name())))
                .signupDate(cursorReader.hasColumn(UsersView.SIGNUP_DATE.name()) ? Optional.of(cursorReader.getDateFromTimestamp(UsersView.SIGNUP_DATE.name())): Optional.absent())
                .country(Optional.fromNullable(cursorReader.getString(UsersView.COUNTRY.name())))
                .city(Optional.fromNullable(cursorReader.getString(UsersView.CITY.name())))
                .followersCount(cursorReader.getInt(UsersView.FOLLOWERS_COUNT.name()))
                .followingsCount(cursorReader.getInt(UsersView.FOLLOWINGS_COUNT.name()))
                .description(Optional.fromNullable(cursorReader.getString(UsersView.DESCRIPTION.name())))
                .avatarUrl(Optional.fromNullable(cursorReader.getString(UsersView.AVATAR_URL.name())))
                .visualUrl(Optional.fromNullable(cursorReader.getString(UsersView.VISUAL_URL.name())))
                .websiteUrl(Optional.fromNullable(cursorReader.getString(UsersView.WEBSITE_URL.name())))
                .websiteName(Optional.fromNullable(cursorReader.getString(UsersView.WEBSITE_NAME.name())))
                .mySpaceName(Optional.fromNullable(cursorReader.getString(UsersView.MYSPACE_NAME.name())))
                .discogsName(Optional.fromNullable(cursorReader.getString(UsersView.DISCOGS_NAME.name())))
                .artistStation(Optional.fromNullable(cursorReader.getString(UsersView.ARTIST_STATION.name())).transform(STRING_TO_URN))
                .isFollowing(cursorReader.getBoolean(UsersView.IS_FOLLOWING.name()))
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
                .avatarUrl(apiUser.getAvatarUrlTemplate())
                .visualUrl(apiUser.getVisualUrlTemplate())
                .websiteUrl(apiUser.getWebsiteUrl())
                .websiteName(apiUser.getWebsiteName())
                .mySpaceName(apiUser.getMyspaceName())
                .discogsName(apiUser.getDiscogsName())
                .artistStation(apiUser.getArtistStationUrn())
                .isFollowing(false)
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

        public abstract Builder isFollowing(boolean isFollowing);

        public abstract User build();
    }
}
