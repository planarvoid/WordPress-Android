package com.soundcloud.android.sync.entities;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.ApiSyncable;

import java.util.Collection;

public abstract class PublishUpdateEvent<T extends ApiSyncable> extends Command<Collection<T>, Boolean> {
}
