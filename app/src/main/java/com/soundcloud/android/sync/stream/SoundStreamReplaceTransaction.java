package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;

public class SoundStreamReplaceTransaction extends SoundStreamInsertTransaction {

    SoundStreamReplaceTransaction(Iterable<ApiStreamItem> streamItems) {
        super(streamItems);
    }

    @Override
    public void steps(PropellerDatabase propeller) {
        step(propeller.delete(Table.SoundStream));
        step(propeller.delete(Table.PromotedTracks));
        super.steps(propeller);
    }

}
