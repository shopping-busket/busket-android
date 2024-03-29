package dev.justme.busket.me.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.justme.busket.R
import dev.justme.busket.feathers.FeathersSocket
import dev.justme.busket.feathers.responses.ShoppingList

typealias ListClickListener = (v: View?, shoppingList: ShoppingList, position: Int) -> Unit

class ListOverviewAdapter(val context: Context, var lists: MutableList<ShoppingList>, val onClick: ListClickListener, val onRemoveClick: ListClickListener) :
    RecyclerView.Adapter<ListOverviewAdapter.ListOverviewHolder>() {
    class ListOverviewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listTitle: TextView = itemView.findViewById(R.id.list_overview_item_title)
        private val listSubtitle: TextView = itemView.findViewById(R.id.list_overview_item_subtitle)
        private val listOverviewCard: CardView = itemView.findViewById(R.id.list_overview_card)
        private val listItemRemoveFromLibrary: Button = itemView.findViewById(R.id.list_item_remove_from_library)

        fun bind(context: Context, shoppingList: ShoppingList, onClick: ListClickListener, onRemoveClick: ListClickListener, position: Int) {
            listTitle.text = shoppingList.name
            listSubtitle.text = shoppingList.description
            listOverviewCard.setOnClickListener {
                onClick.invoke(it, shoppingList, position)
            }

            val user = FeathersSocket.getInstance(context).user?.uuid ?: "error"
            if (user != shoppingList.owner) listItemRemoveFromLibrary.foreground = ContextCompat.getDrawable(context, R.drawable.ic_exit_run)

            listItemRemoveFromLibrary.setOnClickListener {
                onRemoveClick.invoke(it, shoppingList, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListOverviewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_overview_item, parent, false)

        return ListOverviewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    override fun onBindViewHolder(holder: ListOverviewHolder, position: Int) {
        val item = lists[position]
        holder.bind(context, item, onClick, onRemoveClick, position)
    }
}