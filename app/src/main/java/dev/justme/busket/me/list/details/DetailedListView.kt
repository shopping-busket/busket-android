package dev.justme.busket.me.list.details

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.justme.busket.MainActivity
import dev.justme.busket.databinding.FragmentDetailedListViewBinding
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.ShoppingList
import org.json.JSONObject

private const val ARG_LIST_ID = "listId"

class DetailedListView : Fragment() {
    private var _binding: FragmentDetailedListViewBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private var listId: String? = null
    private var list: ShoppingList? = null
    private lateinit var feathers: FeathersSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listId = it.getString(ARG_LIST_ID)
        }
    }

    private fun loadListFromRemote(cb: () -> Unit) {
        val query = JSONObject()
        query.put("listid", listId)
        feathers.service(FeathersSocket.Service.LIST, FeathersSocket.Method.FIND, query) { data, err ->
            if (data == null || err != null) return@service

            val arr = data.getJSONArray(FeathersSocket.ARRAY_DATA_KEY)
            if (arr.length() <= 0) return@service // TODO TRIGGER NOT FOUND

            list = ShoppingList.fromJSONObject(arr.getJSONObject(0))
            handler.post {
                cb.invoke()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailedListViewBinding.inflate(inflater)

        feathers = FeathersSocket.getInstance(requireContext())
        Log.d("Busket DetailedListView", "onCreate.arguments.listId: $listId")

        binding.listContainer.visibility = View.GONE
        binding.listLoader.visibility = View.VISIBLE
        loadListFromRemote {
            (requireActivity() as MainActivity).supportActionBar?.title = list?.name
            binding.listContainer.visibility = View.VISIBLE
            binding.listLoader.visibility = View.GONE
        }

        return binding.root;
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param listId The id of the list to load.
         * @return A new instance of fragment DetailedListView.
         */
        @JvmStatic
        fun newInstance(listId: String) =
            DetailedListView().apply {
                arguments = Bundle().apply {
                    putString(ARG_LIST_ID, listId)
                }
            }
    }
}