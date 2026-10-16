package com.shilapi.xcertplay.update

internal object UpdateVersion {
    private val VERSION = Regex("(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.(\\d+))?")

    internal fun parse(value: String): List<Int>? {
        val match = VERSION.find(value) ?: return null
        return (1..4).map { index ->
            match.groupValues[index].ifEmpty { "0" }.toIntOrNull() ?: return null
        }
    }

    internal fun isNewer(remote: String, installed: String): Boolean {
        val target = parse(remote) ?: return false
        val current = parse(installed) ?: return true
        for (index in target.indices) {
            if (target[index] != current[index]) return target[index] > current[index]
        }
        return false
    }
}
