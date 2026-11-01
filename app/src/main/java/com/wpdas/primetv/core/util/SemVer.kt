package com.wpdas.primetv.core.util

/** Versão semântica simples: "v1.2.3", "1.2.3-dev.4", "1.2". Pré-release conta como menor que a final. */
data class SemVer(val major: Int, val minor: Int, val patch: Int, val preRelease: String? = null) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }).let { if (it != 0) return it }
        return when {
            preRelease == null && other.preRelease == null -> 0
            preRelease == null -> 1
            other.preRelease == null -> -1
            else -> preRelease.compareTo(other.preRelease)
        }
    }

    override fun toString(): String = "$major.$minor.$patch" + (preRelease?.let { "-$it" } ?: "")

    companion object {
        private val PATTERN = Regex("""^v?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.\-]+))?(?:\+.*)?$""")

        fun parse(text: String?): SemVer? {
            val m = PATTERN.find(text?.trim().orEmpty()) ?: return null
            return SemVer(
                major = m.groupValues[1].toInt(),
                minor = m.groupValues[2].toIntOrNull() ?: 0,
                patch = m.groupValues[3].toIntOrNull() ?: 0,
                preRelease = m.groupValues[4].ifEmpty { null },
            )
        }

        /** true quando [remote] é mais nova que [current]. Versões que não parseiam nunca atualizam. */
        fun isNewer(remote: String?, current: String?): Boolean {
            val r = parse(remote) ?: return false
            val c = parse(current) ?: return false
            return r > c
        }
    }
}
