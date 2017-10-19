package com.soundcloud.android.onboarding

import com.soundcloud.android.crypto.Obfuscator
import com.soundcloud.android.utils.OpenForTesting
import javax.inject.Inject

@OpenForTesting
class SignupSignature
@Inject
constructor(val obfuscator: Obfuscator) {
    private val version: Int = 1
    private val secret = "MFx5XmsCEHtBCjMNFVV6AGUhUBVHWxYIBTU3AGxaXzg="

    fun getSignature(email: String, clientId: String): String {
        val payload = listOf(email, clientId, obfuscator.deobfuscateString(secret), version).joinToString(":")
        val bytes = payload.toByteArray()

        val mod: Int = 55439
        var a: Int = 1
        var b: Int = 0
        for (aByte in bytes) {
            a = (a + aByte) % mod
            b = (b + a) % mod
        }

        val result: Int = (b shl 16 or a).inv()

        return listOf(version, Integer.toHexString(result)).joinToString(":")
    }
}
