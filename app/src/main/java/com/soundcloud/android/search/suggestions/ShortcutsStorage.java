package com.soundcloud.android.search.suggestions;

import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.Shortcuts;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ShortcutsStorage {

    private final PropellerDatabase propeller;

    @Inject
    public ShortcutsStorage(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    public List<Shortcut> getShortcuts(String query, int limit) {
        final QueryResult queryResult = propeller.query(from(Shortcuts.TABLE)
                .select(Shortcuts.KIND,
                        Shortcuts._ID,
                        Shortcuts._TYPE,
                        Shortcuts.DISPLAY_TEXT)
                .where(Shortcuts.DISPLAY_TEXT + " LIKE '" + query + "%' or "
                        + Shortcuts.DISPLAY_TEXT + " LIKE '% " + query + "%'")
                .limit(limit));

        List<Shortcut> results = new ArrayList<>(limit);
        for (CursorReader reader : queryResult) {
            results.add(Shortcut.create(getUrn(reader), getDisplayText(reader)));
        }
        return results;
    }

    @NonNull
    private Urn getUrn(CursorReader reader) {
        final long id = reader.getLong(Shortcuts._ID);
        if (Shortcuts.KIND_FOLLOWING.equals(reader.getString(Shortcuts.KIND))){
            return Urn.forUser(id);
        } else if (reader.getInt(Shortcuts._TYPE) == TableColumns.Sounds.TYPE_TRACK){
            return Urn.forTrack(id);
        } else {
            return Urn.forPlaylist(id);
        }
    }

    private String getDisplayText(CursorReader reader) {
        return reader.getString(Shortcuts.DISPLAY_TEXT);
    }

}
