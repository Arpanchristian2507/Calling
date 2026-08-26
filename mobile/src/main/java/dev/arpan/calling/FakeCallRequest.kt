package dev.arpan.calling

internal data class FakeCallRequest(
    val callerName: String,
    val delaySeconds: Int = 0,
    /** When set and in the future, the alarm fires at this wall-clock instant (epoch millis). */
    val scheduledAtMillis: Long? = null,
) {
    fun encode(): ByteArray {
        val cleanCaller = callerName.replace(SEPARATOR, ' ').replace('\n', ' ').trim()
        val trigger = scheduledAtMillis?.takeIf { it > 0L } ?: 0L
        return "$cleanCaller$SEPARATOR${delaySeconds.coerceAtLeast(0)}$SEPARATOR$trigger".encodeToByteArray()
    }

    companion object {
        private const val SEPARATOR: Char = '\u0001'

        fun decode(data: ByteArray?): FakeCallRequest {
            val raw = data?.decodeToString().orEmpty()
            if (raw.isBlank()) {
                return FakeCallRequest(callerName = "")
            }
            val parts = raw.split(SEPARATOR, limit = 3)
            val caller = parts.firstOrNull().orEmpty().trim()
            val delay =
                parts.getOrNull(1)
                    ?.trim()
                    ?.toIntOrNull()
                    ?.coerceAtLeast(0)
                    ?: 0
            val scheduledAt =
                parts.getOrNull(2)
                    ?.trim()
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
            return FakeCallRequest(callerName = caller, delaySeconds = delay, scheduledAtMillis = scheduledAt)
        }
    }
}
