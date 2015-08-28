package com.soundcloud.propeller.query;

import org.jetbrains.annotations.Nullable;

// TODO: we should replace this with a proper Column type. If we had a Column type,
// queries could simply accept Column instances instead of strings.
@Deprecated // use Column to alias columns
public class Field implements ColumnFunction<com.soundcloud.propeller.query.Field> {

    @Nullable private String columnAlias;
    private final String columnName;

    public static Field field(String column) {
        return new Field(column);
    }

    public Field(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String build() {
        final StringBuilder columnBuilder = new StringBuilder(columnName);
        ColumnFunctions.aliasColumn(columnBuilder, columnAlias);
        return columnBuilder.toString();
    }

    @Override
    public Field as(String columnAlias) {
        this.columnAlias = columnAlias;
        return this;
    }
}
