package com.soundcloud.android.provider;

import org.jetbrains.annotations.NotNull;

import android.database.Cursor;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TypeIdList extends ArrayList<TypeId> {

    public TypeIdList(Collection<TypeId> modelTypeIds) {
        super(modelTypeIds);
    }

    public TypeIdList(@NotNull Cursor c) {
        while (c.moveToNext()) {
            add(new TypeId(
                    c.getInt(c.getColumnIndex(DBHelper.ResourceTable._TYPE)),
                    c.getLong(c.getColumnIndex(DBHelper.ResourceTable._ID))
            ));
        }
        c.close();
    }

    public HashMap<Integer, ArrayList<Long>> getIdsByType() {
        HashMap<Integer, ArrayList<Long>> lookupIds = new HashMap<Integer, ArrayList<Long>>();
        for (Pair<Integer, Long> pair : this) {
            if (!lookupIds.containsKey(pair.first)) lookupIds.put(pair.first, new ArrayList<Long>());
            lookupIds.get(pair.first).add(pair.second);
        }
        return lookupIds;
    }


}
