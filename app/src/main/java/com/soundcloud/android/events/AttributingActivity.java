package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class AttributingActivity {
    public static final String POSTED = "posted";
    public static final String REPOSTED = "reposted";
    public static final String PROMOTED = "promoted";

    private static final List<String> modulesWithAttributingActivity = Collections.singletonList(Module.STREAM);

    public static AttributingActivity create(String type, Optional<Urn> resource) {
        return new AutoValue_AttributingActivity(type, resource.isPresent() ? resource.get().toString() : Strings.EMPTY);
    }

    public abstract String getType();

    public abstract String getResource();

    public boolean isActive(Optional<Module> module) {
        return module.isPresent() && isModuleWithAttributingActivity(module.get());
    }

    private boolean isModuleWithAttributingActivity(Module module) {
        return modulesWithAttributingActivity.contains(module.getName());
    }

    public static AttributingActivity fromPlayableItem(PlayableItem playableItem) {
        if (playableItem instanceof PromotedListItem) {
            return AttributingActivity.create(PROMOTED, ((PromotedListItem) playableItem).getPromoterUrn());
        } else if (playableItem.getReposter().isPresent()) {
            return AttributingActivity.create(REPOSTED, playableItem.getReposterUrn());
        } else {
            return AttributingActivity.create(AttributingActivity.POSTED, Optional.of(playableItem.getCreatorUrn()));
        }
    }
}
