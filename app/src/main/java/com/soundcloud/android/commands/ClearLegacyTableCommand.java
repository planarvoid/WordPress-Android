package com.soundcloud.android.commands;

import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;

public class ClearLegacyTableCommand extends DefaultWriteStorageCommand<Table, ChangeResult> {

    @Inject
    protected ClearLegacyTableCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, Table table) {
        return propeller.delete(table);
    }
}
