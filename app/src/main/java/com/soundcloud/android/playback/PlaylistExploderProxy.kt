package com.soundcloud.android.playback

import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.rx.observers.LambdaObserver
import com.soundcloud.android.utils.extensions.plusAssign
import com.soundcloud.rx.eventbus.EventBusV2
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistExploderProxy
@Inject
constructor(internal val eventBus: EventBusV2,
            internal val playlistExploder: Lazy<PlaylistExploder>) {

    private val disposables = CompositeDisposable()

    fun subscribe() {
        disposables += eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, LambdaObserver.onNext { playlistExploder.get().onCurrentPlayQueueItem(it) })
        disposables += eventBus.subscribe(EventQueue.PLAY_QUEUE, LambdaObserver.onNext { playlistExploder.get().onPlayQueue(it) })
    }

}
