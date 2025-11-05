package archives.tater.discordito

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

@Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")
class Ref<T>(val get: () -> T, val set: (T) -> Unit) : ReadWriteProperty<Any?, T> {
    var value: T
        get() = get()
        set(value) { set(value) }

    override inline fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override inline fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

fun <K, V: Any> MutableMap<K, V>.getRef(key: K) = Ref({
    this[key]
}) {
    if (it == null)
        remove(key)
    else
        this[key] = it
}

fun <V> KMutableProperty0<V>.toRef() = Ref({ get() }, { set(it) })

fun <T : Comparable<T>> List<T>.isSorted() = zipWithNext().all { (prev, next) -> prev < next }

