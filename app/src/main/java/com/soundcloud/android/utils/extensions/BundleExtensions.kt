package com.soundcloud.android.utils.extensions

import android.os.Bundle
import com.soundcloud.android.model.Urn

fun Bundle.getUrn(key: String): Urn? {
    val urn = this.getString(key)
    return if (urn != null) Urn(urn) else null
}

fun Bundle.putUrn(key: String, urn: Urn?) {
    this.putString(key, urn?.content)
}
