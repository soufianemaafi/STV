package com.example.soukitv.core.security

object SecretDecoder {
    private const val DEFAULT_KEY: Byte = 0x4B

    fun decode(masked: ByteArray, key: Byte = DEFAULT_KEY): String {
        val result = ByteArray(masked.size) { index ->
            (masked[index].toInt() xor key.toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }
}

