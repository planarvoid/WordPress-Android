package com.soundcloud.android.dao;

import org.jetbrains.annotations.NotNull;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to work around content provider's retarded API.
 * TODO: Remove after migration.
 */
public class ResolverHelper {

    private ResolverHelper() {}

    public static @NotNull List<Long> idCursorToList(Cursor c) {
        if (c == null) return Collections.emptyList();
        List<Long> ids = new ArrayList<Long>(c.getCount());
        while (c.moveToNext()) {
            ids.add(c.getLong(0));
        }
        c.close();
        return ids;
    }

    public static String getWhereInClause(String column, int size){
        StringBuilder sb = new StringBuilder(column).append(" IN (?");
        for (int i = 1; i < size; i++) {
            sb.append(",?");
        }
        sb.append(")");
        return sb.toString();
    }

    public static String[] longListToStringArr(Collection<Long> deletions) {
        int i = 0;
        String[] idList = new String[deletions.size()];
        for (Long id : deletions) {
            idList[i] = String.valueOf(id);
            i++;
        }
        return idList;
    }
}
