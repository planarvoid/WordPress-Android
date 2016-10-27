package com.soundcloud.android.storage;

import com.soundcloud.propeller.schema.BaseTable;

abstract class SCBaseTable extends BaseTable {

    SCBaseTable(String name, PrimaryKey primaryKey) {
        super(name, primaryKey);
    }

    abstract String getCreateSQL();

}
