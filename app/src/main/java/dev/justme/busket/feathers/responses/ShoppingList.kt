package dev.justme.busket.feathers.responses

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ShoppingListItem(
    val id: String,
    val name: String,
)

class ShoppingList(
    val id: Int?,
    @SerializedName("listid") val listId: String?,
    val name: String,
    val description: String,
    val owner: String?,
    val entries: List<ShoppingListItem>,
    val checkedEntries: List<ShoppingListItem>,
) {
    companion object {
        fun fromJSONObject(jsonObject: JSONObject): ShoppingList {
            val list = ShoppingListResponse.fromJSONObject(jsonObject)
            return ShoppingList(list.id, list.listId, list.name, list.description, list.owner, list.entries, list.checkedEntries)
        }
    }

    fun toJSONObject(): JSONObject {
        return toJSONObject(false)
    }

    fun toJSONObject(includeNull: Boolean): JSONObject {
        val jsonObject = JSONObject()

        if (id != null) jsonObject.put("id", id);
        if (listId != null) jsonObject.put("listId", listId)

        jsonObject.put("name", name)
        jsonObject.put("description", description)

        if (owner != null) jsonObject.put("owner", owner)

        jsonObject.put("entries", JSONArray(entries))
        jsonObject.put("checkedEntries", JSONArray(checkedEntries))

        return jsonObject
    }
}

data class ShoppingListResponse(
    val id: Int,

    val backgroundURI: String,
    @SerializedName("listid") val listId: String,
    val name: String,
    val description: String,
    val owner: String,
    val entries: List<ShoppingListItem>,
    val checkedEntries: List<ShoppingListItem>,
) {
    companion object {
        private val gson = Gson()

        fun fromJSONObject(jsonObject: JSONObject): ShoppingListResponse {
            return gson.fromJson(jsonObject.toString(), ShoppingListResponse::class.java)
        }
    }
}
