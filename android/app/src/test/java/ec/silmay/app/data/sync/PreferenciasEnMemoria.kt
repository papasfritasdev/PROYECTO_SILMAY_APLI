package ec.silmay.app.data.sync

import android.content.SharedPreferences

/** SharedPreferences en memoria para pruebas JVM (el android.jar de pruebas solo trae stubs). */
class PreferenciasEnMemoria : SharedPreferences {
    val valores = mutableMapOf<String, Any?>()
    var commits = 0

    override fun getAll(): MutableMap<String, *> = valores.toMutableMap()
    override fun getString(key: String, defValue: String?): String? =
        valores[key] as String? ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        (valores[key] as Set<String>?)?.toMutableSet() ?: defValues

    override fun getInt(key: String, defValue: Int): Int = valores[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long): Long = valores[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = valores[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        valores[key] as Boolean? ?: defValue

    override fun contains(key: String): Boolean = valores.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val cambios = mutableMapOf<String, Any?>()
        private var limpiar = false

        override fun putString(key: String, value: String?) = apply { cambios[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) =
            apply { cambios[key] = values?.toSet() }
        override fun putInt(key: String, value: Int) = apply { cambios[key] = value }
        override fun putLong(key: String, value: Long) = apply { cambios[key] = value }
        override fun putFloat(key: String, value: Float) = apply { cambios[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { cambios[key] = value }
        override fun remove(key: String) = apply { cambios[key] = null }
        override fun clear() = apply { limpiar = true }

        override fun commit(): Boolean {
            if (limpiar) valores.clear()
            cambios.forEach { (k, v) -> if (v == null) valores.remove(k) else valores[k] = v }
            commits++
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
