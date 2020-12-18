package com.rafaelmfer.inappreviewandpurchase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.SkuDetails
import com.google.android.material.card.MaterialCardView

interface OnClickSubscriptionItem {

    fun onClickSubscription(skuDetails: SkuDetails)
}

class SubscriptionsAdapter : RecyclerView.Adapter<SubscriptionsAdapter.ViewHolder>() {

    private var skuDetailsList: List<SkuDetails> = emptyList()
    private var listener: OnClickSubscriptionItem? = null

    fun setListener(listener: OnClickSubscriptionItem) {
        this.listener = listener
    }

    fun addList(list: List<SkuDetails>) {
        skuDetailsList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_subscription, parent, false))

    override fun getItemCount(): Int = skuDetailsList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        skuDetailsList[position].run {

            holder.apply {
                titleTextView.text = title
                priceTextView.text = price

                card.setOnClickListener {
                    listener?.onClickSubscription(this@run)
                }
            }
        }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.subscription_name)
        val priceTextView: TextView = itemView.findViewById(R.id.subscription_price)
        val card: MaterialCardView = itemView.findViewById(R.id.subscription_card)
    }
}