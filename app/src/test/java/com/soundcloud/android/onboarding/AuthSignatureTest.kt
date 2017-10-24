package com.soundcloud.android.onboarding

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.crypto.Obfuscator
import com.soundcloud.android.testsupport.AndroidUnitTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock

@SuppressWarnings("IllegalIdentifier")
class AuthSignatureTest : AndroidUnitTest() {

    private val secret = "this_will_be_a_constant_secret"

    @Mock internal lateinit var obfuscator: Obfuscator

    private lateinit var signature: AuthSignature

    @Before
    fun setUp() {
        signature = AuthSignature(obfuscator)

        whenever(obfuscator.deobfuscateString(ArgumentMatchers.anyString())).thenReturn(secret)
    }

    @Test
    fun `it returns the expected output`() {
        assertEquals("1:964ce5e2", signature.getSignature("foo@bar.com", "90c107ee6da96dae008f11b22b31667c"))
        assertEquals("1:b1f9e668", signature.getSignature("foo@bar.com", "23aa3884168c00fb4f273a05ee963842"))
        assertEquals("1:b235e668", signature.getSignature("bar@foo.com", "23aa3884168c00fb4f273a05ee963842"))
    }
}
