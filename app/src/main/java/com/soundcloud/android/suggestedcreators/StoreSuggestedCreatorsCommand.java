package com.soundcloud.android.suggestedcreators;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Tables.SuggestedCreators;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreatorItem;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreators;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

class StoreSuggestedCreatorsCommand extends DefaultWriteStorageCommand<ApiSuggestedCreators, WriteResult> {

    private final PropellerDatabase propeller;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    public StoreSuggestedCreatorsCommand(PropellerDatabase database, StoreUsersCommand storeUsersCommand) {
        super(database);
        this.propeller = database;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final ApiSuggestedCreators apiSuggestedCreators) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                clearTable();
                step(storeUsersCommand.call(apiSuggestedCreators.getAllUsers()));

                for (ApiSuggestedCreatorItem apiSuggestedCreatorItem : apiSuggestedCreators.getCollection()) {
                    step(insertSuggestedCreator(apiSuggestedCreatorItem));
                }
            }
        });
    }

    private InsertResult insertSuggestedCreator(ApiSuggestedCreatorItem apiSuggestedCreatorItem) {
        return propeller.insert(SuggestedCreators.TABLE, buildSuggestedCreatorsContentValues(apiSuggestedCreatorItem));
    }

    private ContentValues buildSuggestedCreatorsContentValues(ApiSuggestedCreatorItem apiSuggestedCreatorItem) {
        final ContentValues contentValues = new ContentValues();
        final Optional<ApiSuggestedCreator> suggestedCreator = apiSuggestedCreatorItem.getSuggestedCreator();

        if (suggestedCreator.isPresent()) {
            contentValues.put(SuggestedCreators.SEED_USER_ID.name(), suggestedCreator.get().getSeedUser().getId());
            contentValues.put(SuggestedCreators.SUGGESTED_USER_ID.name(), suggestedCreator.get().getSuggestedUser().getId());
            contentValues.put(SuggestedCreators.RELATION_KEY.name(), suggestedCreator.get().getRelationKey());
        }

        return contentValues;
    }

    private void clearTable() {
        propeller.delete(SuggestedCreators.TABLE);
    }
}
