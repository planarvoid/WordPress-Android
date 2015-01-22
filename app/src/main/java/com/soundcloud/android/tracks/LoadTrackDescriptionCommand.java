package com.soundcloud.android.tracks;

import com.soundcloud.android.commands.SingleResourceQueryCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;

import javax.inject.Inject;

public class LoadTrackDescriptionCommand extends SingleResourceQueryCommand<Urn> {

    @Inject
    LoadTrackDescriptionCommand(DatabaseScheduler databaseScheduler) {
        super(databaseScheduler, new TrackDescriptionMapper());
    }

    @Override
    protected Query buildQuery(Urn input) {
        return Query.from(Table.SoundView.name())
                .select(TableColumns.SoundView.DESCRIPTION)
                .whereEq(TableColumns.SoundView._ID, input.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

}
