package net.johnceo.safaricontest.safari

object SafariRules {
    fun strip(text: String) = text.replace(Regex("§."), "").replace(Regex("[\\p{Cf}\\p{Co}]"), "").trim()
}