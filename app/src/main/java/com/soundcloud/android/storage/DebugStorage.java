package com.soundcloud.android.storage;

import com.soundcloud.java.collections.Pair;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

public class DebugStorage {

    private final PropellerRxV2 propellerRxV2;

    @Inject
    DebugStorage(@Named(StorageModule.DEBUG_PROPELLER_RX) PropellerRxV2 propellerRxV2) {
        this.propellerRxV2 = propellerRxV2;
    }

    Observable<Pair<String, Integer>> tableSizes() {
        return propellerRxV2.queryResult("SELECT name FROM sqlite_master WHERE type='table'")
                            .flatMap(this::toTableNamesAndSizes);
    }

    private Observable<Pair<String, Integer>> toTableNamesAndSizes(@NonNull QueryResult cursorReaders) {
        List<String> names = new ArrayList<>();
        for (CursorReader cursorReader : cursorReaders) {
            names.add(cursorReader.getString(0));
        }
        cursorReaders.release();

        return Observable.fromIterable(names).flatMap(this::toTableAndSize);
    }

    private Observable<Pair<String, Integer>> toTableAndSize(@NonNull String tableName) {
        return propellerRxV2.queryResult("select count(*) from " + tableName)
                            .map(cursorReaders -> {
                                Pair<String, Integer> pair = Pair.of(tableName, cursorReaders.iterator().next().getInt(0));
                                cursorReaders.release();
                                return pair;
                            });
    }
}
