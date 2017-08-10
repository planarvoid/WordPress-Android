package com.soundcloud.android.hamcrest;

import static com.soundcloud.android.hamcrest.MatcherAssertEventually.assertThatEventually;
import static com.soundcloud.java.collections.Iterables.getLast;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.utils.Supplier;
import com.soundcloud.java.functions.Function;
import org.hamcrest.Matcher;

import java.util.List;

public abstract class TestAsyncState<ModelType> {

    public <MatcherType> void assertState(Matcher<MatcherType> matcher) {
        assertThatEventually((Supplier<MatcherType>) states(), matcher);
    }

    public <MatcherType> void assertLastState(Function<ModelType, MatcherType> f, Matcher<MatcherType> matcher) {
        assertState(not(empty()));
        assertThatEventually(() -> f.apply(lastState()), matcher);
    }

    public <MatcherType> MatcherType lastState() {
        final List<ModelType> modelTypes = states().get();
        final ModelType last = getLast(modelTypes);
        return (MatcherType) last;
    }

    abstract public Supplier<List<ModelType>> states();
}
