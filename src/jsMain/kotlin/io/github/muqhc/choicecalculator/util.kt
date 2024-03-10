package io.github.muqhc.choicecalculator

fun getUrlParamMap(url: String) =
    url.split('?').getOrNull(1)?.split('&')?.associate {
        val parts = it.split('=')
        val name = parts[0]
        val value = parts[1]
        name to value
    } ?: mapOf()
