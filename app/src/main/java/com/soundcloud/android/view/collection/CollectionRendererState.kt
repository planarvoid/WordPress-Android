package com.soundcloud.android.view.collection

import com.soundcloud.android.model.AsyncLoadingState

data class CollectionRendererState<out ItemT>(val collectionLoadingState: AsyncLoadingState, val items: List<ItemT>)

