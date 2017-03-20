package com.soundcloud.android.hamcrest;

import static com.soundcloud.android.hamcrest.MatcherAssertEventually.assertThatEventually;
import static com.soundcloud.java.collections.Iterables.getLast;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.playlists.NewPlaylistDetailsPresenterIntegrationTest;
import com.soundcloud.android.utils.Function;
import com.soundcloud.android.utils.Supplier;
import org.hamcrest.Matcher;

import java.util.List;

public abstract class TestAsyncState<ModelType> {

    public <MatcherType> void assertState(Matcher<MatcherType> matcher) {
        assertThatEventually((Supplier<MatcherType>) states(), matcher);
    }

    public <MatcherType> void assertState(Function<List<ModelType>, MatcherType> f, Matcher<MatcherType> matcher) {
        System.out.flush();
        assertThatEventually(() -> f.apply(states().get()), matcher);
    }

    public <MatcherType> void assertLastState(Matcher<MatcherType> matcher) {
        assertState(not(empty()));
        assertThatEventually(this::lastState, matcher);
    }

    public <MatcherType> void assertLastState(Function<ModelType, MatcherType> f, Matcher<MatcherType> matcher) {
        assertState(not(empty()));
        assertThatEventually(() -> f.apply(lastState()), matcher);
    }

    private <MatcherType> MatcherType lastState() {
        final List<ModelType> modelTypes = states().get();
        final ModelType last = getLast(modelTypes);
        return (MatcherType) last;
    }

    abstract public Supplier<List<ModelType>> states();
}
