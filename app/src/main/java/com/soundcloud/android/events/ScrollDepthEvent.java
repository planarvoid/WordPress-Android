package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.main.Screen;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AutoValue
public abstract class ScrollDepthEvent extends TrackingEvent {
    public static final String KIND = "list_view_interaction";

    public enum Action {
        START("start"),
        SCROLL_START("scroll_start"),
        SCROLL_STOP("scroll_stop"),
        END("end");

        private final String scrollAction;

        Action(String scrollAction) {
            this.scrollAction = scrollAction;
        }

        public String get() {
            return scrollAction;
        }
    }

    public abstract Screen screen();

    public abstract Action action();

    public abstract int columnCount();

    public abstract List<ItemDetails> earliestItems();

    public abstract List<ItemDetails> latestItems();

    public ItemDetails earliestItem() {
        return Collections.min(earliestItems(), itemDetailsComparator());
    }

    public ItemDetails latestItem() {
        return Collections.max(latestItems(), itemDetailsComparator());
    }

    private static Comparator<ItemDetails> itemDetailsComparator() {
        return (first, second) -> Integer.valueOf(first.position()).compareTo(second.position());
    }

    public static ScrollDepthEvent create(Screen screen, Action action, int columnCount,
                                          List<ItemDetails> earliestItems, List<ItemDetails> latestItems,
                                          Optional<ReferringEvent> referringEvent) {
        return new AutoValue_ScrollDepthEvent.Builder()
                .id(defaultId())
                .timestamp(defaultTimestamp())
                .screen(screen)
                .action(action)
                .columnCount(columnCount)
                .earliestItems(earliestItems)
                .latestItems(latestItems)
                .referringEvent(referringEvent)
                .build();
    }

    @Override
    public ScrollDepthEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_ScrollDepthEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @AutoValue
    public abstract static class ItemDetails {
        public abstract int column();
        public abstract int position();
        public abstract float viewablePercentage();

        public static ItemDetails create(int column, int position, float viewablePercentage) {
            return new AutoValue_ScrollDepthEvent_ItemDetails(column, position, viewablePercentage);
       }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder screen(Screen screen);

        public abstract Builder action(Action action);

        public abstract Builder columnCount(int columnCount);

        public abstract Builder earliestItems(List<ItemDetails> earliestItems);

        public abstract Builder latestItems(List<ItemDetails> latestItems);

        public abstract ScrollDepthEvent build();
    }
}
