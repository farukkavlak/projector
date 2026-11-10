package local.projector.panel

/** System properties via the setprop/getprop tools. Works for apps here because SELinux is permissive. */
object Props {
    fun set(key: String, value: String) {
        Runtime.getRuntime().exec(arrayOf("setprop", key, value)).waitFor()
    }

    fun get(key: String): String =
        Runtime.getRuntime().exec(arrayOf("getprop", key)).inputStream.bufferedReader().readText().trim()
}
