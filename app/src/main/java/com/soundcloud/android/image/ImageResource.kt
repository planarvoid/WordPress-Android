package com.soundcloud.android.image

import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional

interface ImageResource {

    val urn: Urn

    val imageUrlTemplate: Optional<String>

}
