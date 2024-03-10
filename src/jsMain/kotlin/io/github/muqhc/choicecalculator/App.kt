package io.github.muqhc.choicecalculator

import io.kvision.Application
import io.kvision.CoreModule
import io.kvision.BootstrapModule
import io.kvision.BootstrapCssModule
import io.kvision.core.*
import io.kvision.form.number.numericInput
import io.kvision.form.text.Text
import io.kvision.form.text.text
import io.kvision.html.*
import io.kvision.module
import io.kvision.panel.hPanel
import io.kvision.panel.root
import io.kvision.panel.vPanel
import io.kvision.startApplication
import io.kvision.utils.ENTER_KEY
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

typealias IntStateMap = MutableMap<String,UpdatingState<Int>>

class App : Application() {

    companion object {
        val WORTH_DISPLAY_LABEL = "가치"
        val NET_BENEFIT_DISPLAY_LABEL = "순편익"
    }

    val updating = UpdatingSystem()

    val stateContainerMap: MutableMap<String,IntStateMap> = mutableMapOf()

    fun simplify(stateContainerMap: MutableMap<String,IntStateMap>): Map<String, Map<String, Int>> {
        return stateContainerMap.mapValues {
            it.value.mapValues { it.value.get() }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encode(): String {
        val simplified = simplify(stateContainerMap)
        val str = Json.encodeToString(simplified)
        return Base64.UrlSafe.encodeToByteArray(str.encodeToByteArray()).decodeToString()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decode(base64: String): Map<String, Map<String, Int>> {
        return Json.decodeFromString<Map<String, Map<String, Int>>>(
            Base64.UrlSafe.decode(base64.encodeToByteArray()).decodeToString()
        )
    }

    fun Container.gen(source: Map<String, Map<String, Int>>) {
        source.forEach {  (boxName,boxStateMap) ->
            val cCon = choiceBox(boxName)
            boxStateMap.forEach { (name,value) ->
                cCon.choice(name,stateContainerMap[boxName]!!,value)
            }
        }
        updating.update()
    }


    override fun start() {
        root("kvapp") {
            val source = getUrlParamMap(window.location.href)["source"]
            vPanel {
                lateinit var text: Text
                button("share link") {
                    onClick {
                        val origin = window.location.href
                        val ref = origin.split("?")[0]+"?source="+encode()
                        text.value = ref
                    }
                }
                text = text {}
                choiceBoxMaking()
                if (source != null) {
                    gen(decode(source))
                }
            }
        }
    }

    fun Container.choiceBoxMaking() {
        val con = this
        hPanel {
            lateinit var text: Text
            fun add() {
                val name = text.value
                if (name in stateContainerMap.keys || name == null) Unit
                else con.choiceBox(name)
            }
            button("+") {
                onClick { add() }
            }
            text = text {
                placeholder = "name"
                setEventListener<Text> {
                    keydown = {
                        if (it.keyCode == ENTER_KEY) {
                            add()
                        }
                    }
                }
            }
        }
    }

    fun Container.choiceBox(name: String): Container {
        val stateContainer: IntStateMap = mutableMapOf()
        stateContainerMap[name] = stateContainer
        val con = this
        return vPanel {
            hPanel {
                h3(name)
                button("x") {
                    onClick {
                        stateContainer.clear()
                        stateContainerMap.remove(name)
                        con.remove(this@vPanel)
                    }
                }
            }
            h5 {
                updating.updated {
                    val worthSum = stateContainer.values.sumOf { it.get() }
                    val netBenefit = worthSum*2 - stateContainerMap.values.sumOf { it.values.sumOf { it.get() } }
                    content = "[$WORTH_DISPLAY_LABEL: $worthSum][$NET_BENEFIT_DISPLAY_LABEL: $netBenefit]"
                }
            }
            choiceMaking(stateContainer)
        }
    }
    fun Container.choiceMaking(stateMap: IntStateMap) {
        val con = this
        hPanel {
            lateinit var text: Text
            fun add() {
                val name = text.value
                if (name in stateMap.keys || name == null) Unit
                else con.choice(name, stateMap)
            }
            button("+") {
                onClick {
                    add()
                }
            }
            text = text {
                placeholder = "name"
                setEventListener<Text> {
                    keydown = {
                        if (it.keyCode == ENTER_KEY) {
                            add()
                        }
                    }
                }
            }
        }
    }
    fun Container.choice(name: String, stateMap: IntStateMap, startValue: Int = 0) {
        val con = this
        hPanel {
            var worth by updating { state(startValue) }.also { stateMap[name] = it }
            h5(name)
            numericInput(worth, decimals = 0) {
                onInput {
                    (value as Int?)?.let {
                        worth = it
                    }
                }
            }
            button("x") {
                onClick {
                    stateMap.remove(name)
                    con.remove(this@hPanel)
                }
            }
        }
    }
}

fun main() {
    startApplication(
        ::App,
        module.hot,
        BootstrapModule,
        BootstrapCssModule,
        CoreModule
    )
}
