package dev.arpan.calling

internal data class FakeCallRequest(
    val callerName: String,
    val delaySeconds: Int = 0,
) {
    fun encode(): ByteArray {
        val cleanCaller = callerName.replace(SEPARATOR, ' ').replace('\n', ' ').trim()
        return "$cleanCaller$SEPARATOR${delaySeconds.coerceAtLeast(0)}${SEPARATOR}0".encodeToByteArray()
    }

    companion object {
        private const val SEPARATOR: Char = '\u0001'
    }
}
