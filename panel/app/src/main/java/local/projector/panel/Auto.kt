package local.projector.panel

/** Auto keystone and auto focus switches from the OEM settings. */
object Auto {
    fun set(name: String?, on: Boolean): String {
        when (name) {
            "keystone" -> Props.set("persist.sys.tpryxcrt", if (on) "1" else "0")
            "focus" -> Props.set("persist.sys.vafocus", if (on) "1" else "0")
            else -> throw IllegalArgumentException("bilinmeyen anahtar: $name")
        }
        return status()
    }

    fun status(): String =
        """{"keystone":${Props.get("persist.sys.tpryxcrt") == "1"},"focus":${Props.get("persist.sys.vafocus") == "1"}}"""
}
