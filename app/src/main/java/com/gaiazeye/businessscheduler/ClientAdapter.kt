package com.gaiazeye.businessscheduler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

sealed class ClientListItem {
    data class Header(val letter: String) : ClientListItem()
    data class Item(val client: Client) : ClientListItem()
}

class ClientAdapter(
    private var clients: List<Client>,
    private val onClick: (Client) -> Unit,
    private val onLongClick: (Client) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    private var clientsFull = ArrayList(clients)
    private var displayItems: List<ClientListItem> = emptyList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    init {
        updateDisplayList(clients)
    }

    private fun updateDisplayList(list: List<Client>) {
        val sorted = list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.getLastName() })
        val items = mutableListOf<ClientListItem>()
        var lastHeader = ""

        for (client in sorted) {
            val lastName = client.getLastName()
            val firstChar = if (lastName.isNotEmpty()) lastName.substring(0, 1).uppercase(Locale.ROOT) else "#"
            if (firstChar != lastHeader) {
                lastHeader = firstChar
                items.add(ClientListItem.Header(lastHeader))
            }
            items.add(ClientListItem.Item(client))
        }
        displayItems = items
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerTitle: TextView = view.findViewById(R.id.headerTitle)
    }

    class ClientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView = view.findViewById(R.id.clientName)
        val infoTv: TextView = view.findViewById(R.id.clientInfo)
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is ClientListItem.Header -> TYPE_HEADER
            is ClientListItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_client_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_client, parent, false)
            ClientViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val listItem = displayItems[position]) {
            is ClientListItem.Header -> {
                (holder as HeaderViewHolder).headerTitle.text = listItem.letter
            }
            is ClientListItem.Item -> {
                val client = listItem.client
                val itemHolder = holder as ClientViewHolder
                itemHolder.nameTv.text = client.name
                itemHolder.infoTv.text = "${client.email} | ${client.phone}"
                itemHolder.itemView.setOnClickListener { onClick(client) }
                itemHolder.itemView.setOnLongClickListener {
                    onLongClick(client)
                    true
                }
            }
        }
    }

    override fun getItemCount() = displayItems.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList = ArrayList<Client>()
                if (constraint.isNullOrEmpty()) {
                    filteredList.addAll(clientsFull)
                } else {
                    val pattern = constraint.toString().lowercase(Locale.ROOT).trim()
                    for (item in clientsFull) {
                        if (item.name.lowercase(Locale.ROOT).contains(pattern) ||
                            item.email.lowercase(Locale.ROOT).contains(pattern) ||
                            item.phone.lowercase(Locale.ROOT).contains(pattern)
                        ) {
                            filteredList.add(item)
                        }
                    }
                }
                val results = FilterResults()
                results.values = filteredList
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val list = (results?.values as? List<Client>) ?: emptyList()
                updateDisplayList(list)
                notifyDataSetChanged()
            }
        }
    }

    fun updateList(newList: List<Client>) {
        clientsFull = ArrayList(newList)
        updateDisplayList(newList)
        notifyDataSetChanged()
    }
}
