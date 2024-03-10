package io.github.muqhc.choicecalculator

import io.kvision.core.Component
import io.kvision.core.Container
import kotlin.reflect.KProperty

class UpdatingSystem(
    val updaters: MutableList<Pair<Container, () -> Component>> = mutableListOf(),
    val rawUpdaters: MutableList<() -> Unit> = mutableListOf()
) {
    val compoStore: MutableList<Pair<Container, Component>> = mutableListOf()

    fun init() {
        updaters.map { (con,updater) ->
            val com = updater()
            con to com
        }.forEach { (con,com) ->
            con.add(com)
            compoStore += con to com
        }
        rawUpdaters.forEach { it() }
    }
    fun update() {
        compoStore.forEach { (con,com) -> con.remove(com) }
        compoStore.clear()
        init()
    }
    fun Container.component(updater: () -> Component) {
        updaters += this to updater
        val compo = updater()
        compoStore += this to compo
        add(compo)
    }
    fun updated(rawUpdater: () -> Unit) {
        rawUpdater()
        rawUpdaters += rawUpdater
    }
    fun <T> Container.state(data: T) = UpdatingState<T>(this@UpdatingSystem,this,data)
    operator fun <T> invoke(applier: UpdatingSystem.() -> T) = applier()
}

class UpdatingState<T>(val us: UpdatingSystem, val container: Container, private var data: T) {
    fun set(value: T) {
        data = value
        us.update()
    }
    fun get() = data


}

operator fun <T> UpdatingState<T>.getValue(th: Any?, prop: KProperty<*>) = get()
operator fun <T> UpdatingState<T>.setValue(th: Any?, prop: KProperty<*>, value: T) = set(value)



