package com.stv.videoplayer.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PermissionHelperTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var helper: PermissionHelper
    private val packageInfos = mutableMapOf<String, PackageInfo>()

    @Before
    fun setup() {
        context = mockk()
        packageManager = mockk()
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.soufcom.stv"
        every {
            packageManager.getPackageInfo(any<String>(), any<Int>())
        } answers {
            packageInfos[firstArg<String>()]
                ?: throw PackageManager.NameNotFoundException(firstArg<String>())
        }
        helper = PermissionHelper(context)
    }

    @Test
    fun `package sans identite est accepte pour lecture non sensible`() {
        assertTrue(helper.isCallerAuthorizedForPlayer(null))
        assertTrue(helper.isCallerAuthorizedForPlayer(""))
    }

    @Test
    fun `package sans identite est refuse pour extras sensibles`() {
        assertFalse(helper.isCallerAuthorizedForSensitiveExtras(null))
        assertFalse(helper.isCallerAuthorizedForSensitiveExtras(""))
    }

    @Test
    fun `package signe avec le meme certificat est autorise`() {
        val certificate = byteArrayOf(1, 2, 3)
        stubPackage("com.example.signed", certificate)
        stubPackage("com.soufcom.stv", certificate)

        assertTrue(helper.isCallerAuthorizedForPlayer("com.example.signed"))
        assertTrue(helper.isCallerAuthorizedForSensitiveExtras("com.example.signed"))
    }


    @Test
    fun `package inconnu est refuse sans exception`() {
        every {
            packageManager.getPackageInfo("com.unknown", any<Int>())
        } throws PackageManager.NameNotFoundException("unknown")

        assertFalse(helper.isCallerAuthorizedForPlayer("com.unknown"))
        assertFalse(helper.isCallerAuthorizedForSensitiveExtras("com.unknown"))
    }

    private fun stubPackage(packageName: String, certificate: ByteArray) {
        val info = PackageInfo().apply {
            @Suppress("DEPRECATION")
            signatures = arrayOf(Signature(certificate))
        }
        packageInfos[packageName] = info
    }
}

