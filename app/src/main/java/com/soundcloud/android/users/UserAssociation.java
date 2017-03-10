package com.soundcloud.android.users;

import static com.soundcloud.android.storage.Tables.UserAssociations.ADDED_AT;
import static com.soundcloud.android.storage.Tables.UserAssociations.ASSOCIATION_TYPE;
import static com.soundcloud.android.storage.Tables.UserAssociations.POSITION;
import static com.soundcloud.android.storage.Tables.UserAssociations.REMOVED_AT;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;

import java.util.Date;

@AutoValue
public abstract class UserAssociation {
    public abstract Urn userUrn();

    public abstract long position();

    public abstract int associationType();

    public abstract Optional<Date> addedAt();

    public abstract Optional<Date> removedAt();

    public static UserAssociation create(Urn urn,
                                  long position,
                                  int associationType,
                                  Optional<Date> addedAt,
                                  Optional<Date> removedAt) {
        return new AutoValue_UserAssociation(urn, position, associationType, addedAt, removedAt);
    }

    static UserAssociation create(CursorReader reader) {
        Optional<Date> addedAt = Optional.absent();
        if (reader.isNotNull(ADDED_AT.fullName())) {
            addedAt = Optional.of(reader.getDateFromTimestamp(ADDED_AT.fullName()));
        }
        Optional<Date> removedAt = Optional.absent();
        if (reader.isNotNull(REMOVED_AT.fullName())) {
            removedAt = Optional.of(reader.getDateFromTimestamp(REMOVED_AT.fullName()));
        }
        return new AutoValue_UserAssociation(Urn.forUser(reader.getLong(Tables.UsersView.ID.name())),
                                             reader.getLong(POSITION.fullName()),
                                             reader.getInt(ASSOCIATION_TYPE.fullName()),
                                             addedAt,
                                             removedAt);
    }
}
