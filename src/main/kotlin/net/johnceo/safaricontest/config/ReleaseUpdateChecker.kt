package net.johnceo.safaricontest.config

import com.google.gson.JsonParser

/** Refreshable public metadata, independent of Minecraft and network scheduling for regression tests. */
class ReleaseUpdateChecker(
    private val installed: String,
    private val minecraft: String,
    private val fetch: () -> String,
    private val execute: (() -> Unit) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    data class Release(val tag: String, val name: String, val notes: String)
    data class State(val label: String = "Check updates", val release: Release? = null)
    @Volatile var state = State(); private set
    private var running = false
    private var nextCheck = Long.MIN_VALUE

    @Synchronized fun check(force: Boolean = false) {
        if (running || !force && clock() < nextCheck) return
        running = true
        state = state.copy(label = "Checking…")
        execute {
            var success = false
            try {
                val found = newest(fetch(), minecraft) ?: error("No compatible stable release")
                val comparison = compareVersions(found.tag, installed) ?: error("Unknown installed version")
                state = State(when {
                    comparison > 0 -> "Update available"
                    comparison < 0 -> "Development build"
                    else -> "Up to Date"
                }, found)
                success = true
            } catch (_: Exception) {
                state = state.copy(label = "Couldn't check")
            } finally {
                synchronized(this) {
                    nextCheck = clock() + if (success) 300_000L else 60_000L
                    running = false
                }
            }
        }
    }

    companion object {
        fun newest(body: String, minecraft: String): Release? = JsonParser.parseString(body).asJsonArray
            .map { it.asJsonObject }.mapNotNull { row ->
                val tag = row.get("tag_name")?.takeUnless { it.isJsonNull }?.asString ?: return@mapNotNull null
                if (compareVersions(tag, tag) == null || row.get("draft")?.asBoolean == true ||
                    row.get("prerelease")?.asBoolean == true) return@mapNotNull null
                val version = tag.removePrefix("v")
                if (row.getAsJsonArray("assets")?.any {
                        it.asJsonObject.get("name")?.asString == "safari-contest-$version+mc$minecraft.jar"
                    } != true) return@mapNotNull null
                Release(tag, row.get("name")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
                    row.get("body")?.takeUnless { it.isJsonNull }?.asString.orEmpty())
            }.maxWithOrNull { a, b -> compareVersions(a.tag, b.tag)!! }

        fun compareVersions(a: String, b: String): Int? {
            val pattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$")
            fun parts(value: String) = pattern.matchEntire(value.trim())?.groupValues?.drop(1)
            fun numeric(x: String, y: String): Int {
                val left = x.trimStart('0').ifEmpty { "0" }
                val right = y.trimStart('0').ifEmpty { "0" }
                return left.length.compareTo(right.length).takeIf { it != 0 } ?: left.compareTo(right)
            }
            val x = parts(a) ?: return null
            val y = parts(b) ?: return null
            for (i in 0..2) numeric(x[i], y[i]).takeIf { it != 0 }?.let { return it }
            if (x[3].isEmpty() || y[3].isEmpty()) {
                return when {
                    x[3] == y[3] -> 0
                    x[3].isEmpty() -> 1
                    else -> -1
                }
            }
            val left = x[3].split('.')
            val right = y[3].split('.')
            for (i in 0 until minOf(left.size, right.size)) {
                val leftNumeric = left[i].all(Char::isDigit)
                val rightNumeric = right[i].all(Char::isDigit)
                val comparison = when {
                    leftNumeric && rightNumeric -> numeric(left[i], right[i])
                    leftNumeric -> -1
                    rightNumeric -> 1
                    else -> left[i].compareTo(right[i])
                }
                if (comparison != 0) return comparison
            }
            return left.size.compareTo(right.size)
        }
    }
}
