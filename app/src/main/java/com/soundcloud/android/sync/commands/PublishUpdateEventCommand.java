package com.soundcloud.android.sync.commands;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.ApiSyncable;

import java.util.Collection;

public abstract class PublishUpdateEventCommand<T extends ApiSyncable> extends Command<Collection<T>, Boolean> {
}
