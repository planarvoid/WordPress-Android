package com.soundcloud.android.users;

import static com.soundcloud.android.model.Urn.STRING_TO_URN;
import static com.soundcloud.android.model.Urn.forUser;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.UsersView;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;

@AutoValue
public abstract class User {

    public abstract Urn urn();
    public abstract String username();
    public abstract String country();
    public abstract String city();
    public abstract int followersCount();
    public abstract Optional<String> description();
    public abstract Optional<String> avatarUrl();
    public abstract Optional<String> websiteUrl();
    public abstract Optional<String> websiteName();
    public abstract Optional<String> mySpaceName();
    public abstract Optional<String> discogsName();
    public abstract Optional<Urn> artistStation();
    public abstract boolean isFollowing();

    public static User fromCursorReader(CursorReader cursorReader) {
       return new AutoValue_User(
               forUser(cursorReader.getLong(UsersView.ID.name())),
               cursorReader.getString(UsersView.USERNAME.name()),
               cursorReader.getString(UsersView.COUNTRY.name()),
               cursorReader.getString(UsersView.CITY.name()),
               cursorReader.getInt(UsersView.FOLLOWERS_COUNT.name()),
               Optional.fromNullable(cursorReader.getString(UsersView.DESCRIPTION.name())),
               Optional.fromNullable(cursorReader.getString(UsersView.AVATAR_URL.name())),
               Optional.fromNullable(cursorReader.getString(UsersView.WEBSITE_URL.name())),
               Optional.fromNullable(cursorReader.getString(UsersView.WEBSITE_NAME.name())),
               Optional.fromNullable(cursorReader.getString(UsersView.MYSPACE_NAME.name())),
               Optional.fromNullable(cursorReader.getString(UsersView.DISCOGS_NAME.name())),
               Optional.fromNullable(cursorReader.getString(UsersView.ARTIST_STATION.name())).transform(STRING_TO_URN),
               cursorReader.getBoolean(UsersView.IS_FOLLOWING.name()));
    }

    public static User create(Urn urn,
                              String username,
                              String country,
                              String city,
                              int followersCount,
                              boolean isFollowing) {
        return new AutoValue_User(urn,
                                  username,
                                  country,
                                  city,
                                  followersCount,
                                  Optional.<String>absent(),
                                  Optional.<String>absent(),
                                  Optional.<String>absent(),
                                  Optional.<String>absent(),
                                  Optional.<String>absent(),
                                  Optional.<String>absent(),
                                  Optional.<Urn>absent(),
                                  isFollowing);
    }
}
