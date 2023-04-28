package dev.justme.busket.feathers

import io.socket.client.Ack
import org.json.JSONArray
import org.json.JSONObject

typealias FeathersCallback = (data: JSONObject?, error: SocketError?) -> Unit;

class FeathersService(private val feathers: FeathersSocket, val path: String) {
    constructor(feathers: FeathersSocket, path: Service) : this(feathers, path.toString())

    enum class Method {
        FIND, GET, CREATE, UPDATE, PATCH, REMOVE
    }

    enum class SocketEventListener(val method: String) {
        CREATED("created"),
        UPDATED("updated"),
        PATCHED("patched"),
        REMOVED("removed"),
    }

    enum class Service(val path: String) {
        EVENT("event"),
        LIST("list"),
        LIBRARY("library"),
        WHITELISTED_USERS("whitelisted-users"),
        USERS("users"),
        AUTHENTICATION("authentication"),
    }

    fun create(data: JSONObject,  cb: FeathersCallback) {
        emit(Method.CREATE, arrayOf(data), cb)
    }

    fun update() {}

    fun patch() {}

    fun find(query: JSONObject?, cb: FeathersCallback) {
        emit(Method.FIND, arrayOf(query), cb)
    }

    fun get(id: Int, cb: FeathersCallback) {
        emit(Method.GET, arrayOf(id), cb)
    }

    fun remove(id: Int, cb: FeathersCallback) {
        emit(Method.REMOVE, arrayOf(id), cb)
    }
    fun remove(query: JSONObject?, cb: FeathersCallback) {
        emit(Method.GET, arrayOf(null, query), cb)
    }

    fun on() {}

    /**
     * Emit an event via socket
     *
     * @property method the CRUD method to use
     * @property args array formatted { data, query }
     */
    private fun emit(
        method: Method,
        args: Array<Any?>,
        callback: FeathersCallback
    ) {
        emit(method.name, args, callback)
    }

    /**
     * Emit an event via socket
     *
     * @property method the CRUD method to use
     * @property args array formatted { data, query }
     */
    private fun emit(
        method: String,
        args: Array<Any?>,
        callback: FeathersCallback
    ) {
        /*if (data !is JSONArray && data !is JSONObject && data !is String && data != null) {
            throw Exception("data can either be JSONARRAY, JSONOBJECT or String! Cannot be of type ${data::class.java.typeName}")
        }*/

        feathers.requireConnected {
            feathers.socket.emit(method.lowercase(), path.lowercase(), *args, Ack {
                for (res in it) {
                    if (res != null) {
                        var out = JSONObject()
                        var statusCode = 200
                        if (res is JSONObject) {
                            out = res
                            if (res.has("code")) statusCode = res.getInt("code")
                        } else if (res is JSONArray) {
                            out = JSONObject().put("arrayData", res)
                        }
                        if (!feathers.isSuccessCode(statusCode)) {
                            val errorObj = feathers.gson.fromJson(res.toString(), SocketError::class.java)
                            callback.invoke(null, errorObj)
                            return@Ack
                        }

                        callback.invoke(out, null)
                        return@Ack
                    }
                }
            })
        }
    }
}