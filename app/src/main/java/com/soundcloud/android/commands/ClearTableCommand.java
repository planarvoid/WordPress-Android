package com.soundcloud.android.commands;

import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.schema.Table;

import javax.inject.Inject;

public class ClearTableCommand extends DefaultWriteStorageCommand<Table, ChangeResult> {

    @Inject
    protected ClearTableCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, Table table) {
        return propeller.delete(table);
    }
}
