package com.soundcloud.android.model

import java.util.Date

data class Association(val urn:Urn, val createdAt:Date): UrnHolder {
    override fun urn(): Urn = urn
}
