package com.soundcloud.android.viewstate;


import static com.soundcloud.java.optional.Optional.of;

import com.soundcloud.android.view.AsyncViewModel;
import com.soundcloud.android.view.ViewError;

public interface PartialState<ViewModel> {

    AsyncViewModel<ViewModel> createNewState(AsyncViewModel<ViewModel> oldState);

    class Error<ViewModel> implements PartialState<ViewModel> {
        private final Throwable throwable;

        public Error(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public AsyncViewModel<ViewModel> createNewState(AsyncViewModel<ViewModel> oldState) {
            return AsyncViewModel.create(oldState.data(), false, of(ViewError.from(throwable)));
        }
    }

    class RefreshStarted<ViewModel> implements PartialState<ViewModel> {
        @Override
        public AsyncViewModel<ViewModel> createNewState(AsyncViewModel<ViewModel> oldState) {
            return AsyncViewModel.create(oldState.data(), true, oldState.error());
        }
    }
}