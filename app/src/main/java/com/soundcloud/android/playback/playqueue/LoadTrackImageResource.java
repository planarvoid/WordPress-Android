package com.soundcloud.android.playback.playqueue;

import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackArtwork;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;

class LoadTrackImageResource extends Command<Urn, ImageResource> {

    private final PropellerDatabase database;

    @Inject
    LoadTrackImageResource(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public TrackArtwork call(Urn input) {
        final Query query =
                from(Table.SoundView.name())
                        .select(TableColumns.SoundView._ID, TableColumns.SoundView.ARTWORK_URL)
                        .whereEq(TableColumns.SoundView._ID, input.getNumericId())
                        .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);

        return database.query(query).first(new RxResultMapper<TrackArtwork>() {
            @Override
            public TrackArtwork map(CursorReader reader) {
                return TrackArtwork.create(
                        Urn.forTrack(reader.getLong(TableColumns.SoundView._ID)),
                        Optional.fromNullable(reader.getString(TableColumns.SoundView.ARTWORK_URL))
                );
            }
        });
    }
}
