package projekt.cloud.piece.server

import com.google.gson.JsonObject

const val CONTENT_TYPE = "application/json"
private const val METHOD = "method"
private const val RESULT_CODE = "result_code"
private const val RESPONSE = "response"

fun createJson(method: String, resultCode: String, response: JsonObject? = null) = JsonObject().apply {
    addProperty(METHOD, method)
    addProperty(RESULT_CODE, resultCode)
    response?.let { add(RESPONSE, it) }
}.toString()

fun createJson(method: String, resultCode: String, response: JsonObject.() -> Unit) =
    createJson(method, resultCode, JsonObject().apply(response))