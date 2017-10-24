package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.users.UserRecordHolder;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class ApiUser implements ApiEntityHolder, UserRecord, UserRecordHolder, ApiSyncable {

    private static final String DISCOGS_PATH = "http://www.discogs.com/artist/";
    private static final String DISCOGS_NETWORK = "discogs";
    private static final String MYSPACE_PATH = "http://www.myspace.com/";
    private static final String MYSPACE_NETWORK = "myspace";
    private static final String PERSONAL_NETWORK = "personal";

    @JsonCreator
    public static ApiUser create(
            @JsonProperty("urn") Urn urn,
            @JsonProperty("permalink") String permalink,
            @JsonProperty("username") String username,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("avatar_url") String imageUrlTemplate,
            @JsonProperty("city") String city,
            @JsonProperty("country") String country,
            @JsonProperty("country_code") String countryCode,
            @JsonProperty("tracks_count") int trackCount,
            @JsonProperty("followers_count") int followersCount,
            @JsonProperty("followings_count") int followingsCount,
            @JsonProperty("verified") boolean verified,
            @JsonProperty("is_pro") boolean isPro,
            @JsonProperty("visual_url_template") String visualUrlTemplate,
            @JsonProperty("created_at") Date createdAt,
            @JsonProperty("station_urns") List<Urn> stationsUrns,
            @JsonProperty("social_media_links") ModelCollection<ApiSocialMediaLink> apiSocialMediaLinks) {

        Builder builder = builder(urn).permalink(permalink)
                                      .username(username)
                                      .firstName(Optional.fromNullable(firstName))
                                      .lastName(Optional.fromNullable(lastName))
                                      .imageUrlTemplate(Optional.fromNullable(imageUrlTemplate))
                                      .city(city)
                                      .country(country)
                                      .countryCode(Optional.fromNullable(countryCode))
                                      .trackCount(trackCount)
                                      .followersCount(followersCount)
                                      .followingsCount(followingsCount)
                                      .visualUrlTemplate(Optional.fromNullable(visualUrlTemplate))
                                      .createdAt(createdAt)
                                      .verified(verified)
                                      .pro(isPro)
                                      .artistStationUrn(extractArtistStationUrn(stationsUrns));

        // this converts new social links to the legacy format, and should be removed when we fully move
        // to the new format in https://soundcloud.atlassian.net/browse/CREATORS-2240
        if (apiSocialMediaLinks != null) {
            for (ApiSocialMediaLink apiSocialMediaLink : apiSocialMediaLinks) {
                if (MYSPACE_NETWORK.equals(apiSocialMediaLink.network())) {
                    builder.myspaceName(Optional.of(apiSocialMediaLink.url().replace(MYSPACE_PATH, Strings.EMPTY)));
                } else if (DISCOGS_NETWORK.equals(apiSocialMediaLink.network())) {
                    builder.discogsName(Optional.of(apiSocialMediaLink.url().replace(DISCOGS_PATH, Strings.EMPTY)));
                } else if (PERSONAL_NETWORK.equals(apiSocialMediaLink.network())) {
                    builder.websiteUrl(Optional.of(apiSocialMediaLink.url()));
                    builder.websiteName(Optional.fromNullable(apiSocialMediaLink.title().orNull()));
                }
            }
        }
        return builder.build();
    }


    private static Optional<Urn> extractArtistStationUrn(@Nullable List<Urn> stations) {
        if (stations != null) {
            for (Urn stationUrn : stations) {
                if (stationUrn.isArtistStation()) {
                    return Optional.of(stationUrn);
                }
            }
        }
        return Optional.absent();
    }

    public static Builder builder(Urn urn) {
        return new AutoValue_ApiUser.Builder()
                .urn(urn)
                .firstName(Optional.absent())
                .lastName(Optional.absent())
                .createdAt(Optional.absent())
                .countryCode(Optional.absent())
                .description(Optional.absent())
                .imageUrlTemplate(Optional.absent())
                .visualUrlTemplate(Optional.absent())
                .websiteUrl(Optional.absent())
                .websiteName(Optional.absent())
                .discogsName(Optional.absent())
                .myspaceName(Optional.absent())
                .artistStationUrn(Optional.absent());
    }

    @Override
    public abstract Urn getUrn();

    public long getId() {
        return getUrn().getNumericId();
    }

    @Override
    public abstract String getPermalink();

    @Override
    public abstract String getUsername();

    @Override
    public abstract Optional<String> getImageUrlTemplate();

    @Override
    public abstract Optional<String> getVisualUrlTemplate();

    @Override
    public abstract Optional<String> getFirstName();

    @Override
    public abstract Optional<String> getLastName();

    @Override
    public abstract Optional<Date> getCreatedAt();

    @Override
    @Nullable
    public abstract String getCountry();

    @Nullable
    @Override
    public abstract String getCity();

    @Override
    public abstract int getFollowersCount();

    @Override
    public abstract Optional<String> getDescription();

    @Override
    public abstract Optional<String> getWebsiteUrl();

    @Override
    public abstract Optional<String> getWebsiteName();

    @Override
    public abstract Optional<String> getDiscogsName();

    @Override
    public abstract Optional<String> getMyspaceName();

    @Override
    public abstract Optional<Urn> getArtistStationUrn();

    @Override
    public UserRecord getUserRecord() {
        return this;
    }

    public abstract Optional<String> getCountryCode();

    public abstract int getTrackCount();

    @Override
    public abstract int getFollowingsCount();

    public abstract boolean isVerified();

    @Override
    public abstract boolean isPro();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder urn(Urn urn);

        public abstract Builder permalink(String permalink);

        public abstract Builder username(String username);

        public abstract Builder imageUrlTemplate(Optional<String> imageUrlTemplate);

        public abstract Builder visualUrlTemplate(Optional<String> visualUrlTemplate);

        public abstract Builder firstName(Optional<String> firstName);

        public abstract Builder lastName(Optional<String> lastName);

        public abstract Builder createdAt(Optional<Date> createdAt);

        public Builder createdAt(Date createdAt) {
            return createdAt(Optional.of(createdAt));
        }

        public abstract Builder country(String country);

        public abstract Builder city(String city);

        public abstract Builder followersCount(int followersCount);

        public abstract Builder description(Optional<String> description);

        public abstract Builder websiteUrl(Optional<String> websiteUrl);

        public abstract Builder websiteName(Optional<String> websiteName);

        public abstract Builder discogsName(Optional<String> discogsName);

        public abstract Builder myspaceName(Optional<String> myspaceName);

        public abstract Builder artistStationUrn(Optional<Urn> artistStationUrn);

        public abstract Builder countryCode(Optional<String> countryCode);

        public abstract Builder trackCount(int trackCount);

        public abstract Builder followingsCount(int followingsCount);

        public abstract Builder verified(boolean isVerified);

        public abstract Builder pro(boolean isPro);

        public abstract ApiUser build();

    }
}
