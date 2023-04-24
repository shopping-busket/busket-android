package dev.justme.busket.feathers

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import dev.justme.busket.helpers.SingletonHolder
import dev.justme.busket.feathers.responses.AuthenticationSuccessResponse
import dev.justme.busket.feathers.responses.User
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONArray
import org.json.JSONObject


data class SocketError(
    val name: String,
    val message: String,
    val code: Int,
    val className: String,

    val data: Any?,
    val errors: List<Any>?,
)

class FeathersSocket(private val context: Context) {
    //region privates
    private val options = IO.Options()
    private val mainKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private var authentication: AuthenticationSuccessResponse? = null
    //endregion

    //region publics
    companion object : SingletonHolder<FeathersSocket, Context>(::FeathersSocket) {
        private const val TAG = "Busket Socket"
        const val ARRAY_DATA_KEY = "arrayData"
    }

    val gson = Gson()
    val socket: Socket = IO.socket("http://localhost:3030", options)
    var user: User? = null
    //endregions

    enum class Method {
        FIND, GET, CREATE, UPDATE, PATCH, REMOVE
    }

    enum class Service(val path: String) {
        EVENT("event"),
        LIST("list"),
        LIBRARY("library"),
        WHITELISTED_USERS("whitelisted-users"),
        USERS("users"),
    }

    //region connection
    private fun connect(connectedCallback: (() -> Unit)?) {
        options.path = "/socket.io/"
        options.transports = arrayOf(WebSocket.NAME)
        socket.connect();

        socket.io().on(Manager.EVENT_RECONNECT) {
            tryAuthenticateWithAccessToken({
                connectedCallback?.invoke()
            }, {
                connectedCallback?.invoke()
            })
        }

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected. Invoking callback")
            connectedCallback?.invoke()
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected.")
            socket.connect()
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(TAG, "Socket error.")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d(TAG, "Socket connection error.")
        }
    }

    fun requireConnected(fn: () -> Unit) {
        if (!socket.connected()) connect(fn)
        else fn.invoke()
    }
    //endregion

    //region service methods
    fun service(
        name: Service,
        method: Method,
        data: JSONObject?,
        callback: (data: JSONObject?, error: SocketError?) -> Unit
    ) {
        service(name.path, method.toString(), data, callback)
    }

    fun service(
        name: String,
        method: Method,
        data: JSONObject?,
        callback: (data: JSONObject?, error: SocketError?) -> Unit
    ) {
        service(name, method.toString(), data, callback)
    }

    fun isSuccessCode(statusCode: Int): Boolean {
        var code = statusCode
        while (code > 9) code /= 10
        return code == 2
    }

    fun service(
        name: String,
        method: String,
        data: JSONObject?,
        callback: (data: JSONObject?, error: SocketError?) -> Unit
    ) {
        requireConnected {
            socket.emit(method.lowercase(), name.lowercase(), data ?: JSONObject(), Ack {
                var foundResponse = false
                for (res in it) {
                    if (res != null) {
                        foundResponse = true

                        var out = JSONObject()
                        var statusCode = 200
                        if (res is JSONObject) {
                            out = res
                            if (res.has("code")) statusCode = res.getInt("code")
                        } else if (res is JSONArray) {
                            out = JSONObject().put("arrayData", res)
                        }
                        if (!isSuccessCode(statusCode)) {
                            val errorObj = gson.fromJson(res.toString(), SocketError::class.java)
                            callback.invoke(null, errorObj)
                            return@Ack
                        }

                        callback.invoke(out, null)
                        return@Ack
                    }
                }

                if (!foundResponse) {
                    callback.invoke(
                        null,
                        SocketError(
                            "Unknown error",
                            "An unknown networking error occurred",
                            -1,
                            "",
                            null,
                            null
                        )
                    )
                    return@Ack
                }
            })
        }
    }
    //endregion

    //region authentication
    fun authenticate(
        email: String,
        password: String,
        successCallback: (AuthenticationSuccessResponse) -> Unit,
        errorCallback: ((e: SocketError) -> Unit)? = null,
    ) {
        fun success(auth: AuthenticationSuccessResponse) {
            storeAccessTokenAndSetUser(auth)
            successCallback(auth)
        }

        tryAuthenticateWithAccessToken({ success(it) }, {
            authenticateWithCredentials(email, password, {
                success(it)
            }, {
                errorCallback?.invoke(it)
            })
        })
    }

    private fun authenticateWithCredentials(
        email: String,
        password: String,
        successCallback: ((AuthenticationSuccessResponse) -> Unit)? = null,
        errorCallback: ((e: SocketError) -> Unit)? = null,
        storeTokenAndUser: Boolean = true,
    ) {
        val jData = JSONObject();
        jData.put("strategy", "local")
        jData.put("email", email)
        jData.put("password", password)

        service("authentication", Method.CREATE, jData) { data, err ->
            if (err != null) {
                errorCallback?.invoke(err)
                return@service
            }

            val auth = gson.fromJson(
                data.toString(), AuthenticationSuccessResponse::class.java
            )
            if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
            successCallback?.invoke(auth)
        }
    }

    fun tryAuthenticateWithAccessToken(
        successCallback: ((AuthenticationSuccessResponse) -> Unit)? = null,
        errorCallback: ((e: SocketError) -> Unit)? = null,
        storeTokenAndUser: Boolean = true,
    ) {
        val storedAccessToken = EncryptedSharedPreferences.create(
            context,
            "encrypted_shared_prefs",
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).getString("access_token", null)

        if (storedAccessToken != null) {
            val jData = JSONObject()
            jData.put("strategy", "jwt");
            jData.put("accessToken", storedAccessToken);

            service("authentication", Method.CREATE, jData) { data, err ->
                if (err != null) {
                    errorCallback?.invoke(err)
                    return@service
                }

                val auth = gson.fromJson(
                    data.toString(), AuthenticationSuccessResponse::class.java
                )
                if (storeTokenAndUser) storeAccessTokenAndSetUser(auth)
                successCallback?.invoke(auth)
            }
        } else {
            errorCallback?.invoke(
                SocketError(
                    "forbidden",
                    "No accesstoken found in SharedPreferences",
                    403,
                    "Forbidden",
                    null,
                    null,
                )
            )
        }
    }

    private fun storeAccessTokenAndSetUser(auth: AuthenticationSuccessResponse) {
        EncryptedSharedPreferences.create(
            context,
            "encrypted_shared_prefs",
            mainKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).edit().putString("access_token", auth.accessToken).apply()

        authentication = auth
        user = auth.user
    }

    fun getAuthentication(): AuthenticationSuccessResponse? {
        return authentication
    }
    //endregion
}