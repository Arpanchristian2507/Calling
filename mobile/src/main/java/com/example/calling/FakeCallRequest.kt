package com.example.calling

internal data class FakeCallRequest(
    val callerName: String,
    val delaySeconds: Int = 0,
) {
    fun encode(): ByteArray {
        val cleanCaller = callerName.replace(SEPARATOR, ' ').replace('\n', ' ').trim()
        return "$cleanCaller$SEPARATOR${delaySeconds.coerceAtLeast(0)}".encodeToByteArray()
    }

    companion object {
        private const val SEPARATOR: Char = '\u0001'

        fun decode(data: ByteArray?): FakeCallRequest {
            val raw = data?.decodeToString().orEmpty()
            if (raw.isBlank()) {
                return FakeCallRequest(callerName = "")
            }
            val parts = raw.split(SEPARATOR, limit = 2)
            val caller = parts.firstOrNull().orEmpty().trim()
            val delay =
                parts.getOrNull(1)
                    ?.trim()
                    ?.toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?: 0
            return FakeCallRequest(callerName = caller, delaySeconds = delay)
        }
    }
}
