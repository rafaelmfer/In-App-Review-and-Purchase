package com.rafaelmfer.inappreviewandpurchase

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import kotlinx.android.synthetic.main.activity_in_app_purchase.*
import java.io.IOException

class InAppPurchaseActivity : AppCompatActivity(), OnClickSubscriptionItem {

    private var billingClient: BillingClient? = null
    private val subscriptionAdapter = SubscriptionsAdapter()
    private var itemPurchased = ""

    companion object {
        fun startScreen(context: Context) {
            context.startActivity(Intent(context, InAppPurchaseActivity::class.java))
        }

        private const val SUBSCRIPTION_ANNUAL = "teste_pacote_premium"
        private const val SUBSCRIPTION_MONTHLY = "test_subscription_premium_mensal"
        private const val PREF_FILE = "MyPref"
        private const val PURCHASE_KEY = "purchase"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_purchase)
        setupBillingClient()

        setupRecyclerView()
        bindClicks()


    }

    override fun onResume() {
        super.onResume()
        checkConnection(action = {
            val queryProducts = billingClient?.queryPurchases(BillingClient.SkuType.INAPP)
            val querySubscriptions = billingClient?.queryPurchases(BillingClient.SkuType.SUBS)
            val queryPurchases = mutableListOf<Purchase>()
            queryProducts?.purchasesList?.let {
                queryPurchases.addAll(it)
            }
            querySubscriptions?.purchasesList?.let {
                queryPurchases.addAll(it)
            }
            if (queryPurchases.size > 0) {
                handlePurchases(queryPurchases)
            } else {
                savePurchaseValueToPref(false)
            }
        })
        //item Purchased
        if (purchaseValueFromPref) {
            subscription_status.text = itemPurchased
        } else {
            subscription_status.text = "Nada Comprado"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingClient != null) {
            billingClient?.endConnection()
        }
    }

    private fun setupRecyclerView() {
        subscriptions_recycler.adapter = subscriptionAdapter
        subscriptionAdapter.setListener(this)

    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchaseUpdateListener)
            .enablePendingPurchases()
            .build()
        startConnection()
    }

    private fun startConnection(action: () -> Unit = {}) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.v("TAG_INAPP", "Setup Billing Done")
                    // The BillingClient is ready. You can query purchases here.
                    action()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.v("TAG_INAPP", "Billing client Disconnected")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun checkConnection(action: () -> Unit = {}) {
        if (billingClient?.isReady == true) {
            action()
        } else {
            startConnection { action() }
        }
    }

    private fun bindClicks() {
        button_load_products.setOnClickListener { checkConnection { queryAvailableProducts() } }
        button_load_subscriptions.setOnClickListener { checkConnection { queryAvailableSubscriptions() } }
    }

    private fun queryAvailableProducts() {
        val skuList = listOf("test.premium", "test.basic")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient?.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            // Process the result.
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                Log.v("TAG_INAPP", "skuDetailsList : $skuDetailsList")
                //This list should contain the products added above
                updateUI(skuDetailsList)
            } else {
                toast(" Error " + billingResult.debugMessage)
            }
        }
    }

    private fun queryAvailableSubscriptions() {
        val skuList = listOf("teste_pacote_premium", "test_subscription_premium_mensal")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)

        billingClient?.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            // Process the result.
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                Log.v("TAG_INAPP", "skuDetailsList : $skuDetailsList")
                //This list should contain the products added above
                subscriptionAdapter.addList(skuDetailsList)
                subscriptionAdapter.notifyDataSetChanged()
            } else {
                toast(" Error " + billingResult.debugMessage)
            }
        }
    }

    private fun updateUI(skuDetailsList: MutableList<SkuDetails>) {
        skuDetailsList[0].apply {
            product_one.text = "${this.title}    ${this.price}"
            product_one.setOnClickListener {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(this)
                    .build()
                billingClient?.launchBillingFlow(this@InAppPurchaseActivity, billingFlowParams)?.responseCode
            }
        }

        skuDetailsList[1].apply {
            product_two.text = "${this.title}    ${this.price}"
            product_two.setOnClickListener {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(this)
                    .build()
                billingClient?.launchBillingFlow(this@InAppPurchaseActivity, billingFlowParams)?.responseCode
            }
        }
    }

    private val purchaseUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.v("TAG_INAPP", "billingResult responseCode : ${billingResult.responseCode}")
        when {
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                handlePurchases(purchases)
            }
            billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                val queryAlreadyPurchasesResult = billingClient?.queryPurchases(BillingClient.SkuType.INAPP)
                val queryAlreadySubscriptionsResult = billingClient?.queryPurchases(BillingClient.SkuType.SUBS)
                val alreadyPurchases = mutableListOf<Purchase>()
                queryAlreadyPurchasesResult?.purchasesList?.let { alreadyPurchases.addAll(it) }
                queryAlreadySubscriptionsResult?.purchasesList?.let { alreadyPurchases.addAll(it) }

                handlePurchases(alreadyPurchases)
            }
            billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                toast("Purchase Canceled")
            }
            else -> {
                toast(" Error " + billingResult.debugMessage)
            }
        }
    }

    private fun handleConsumedPurchases(purchase: Purchase) {
        Log.d("TAG_INAPP", "handleConsumablePurchasesAsync foreach it is $purchase")
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient?.consumeAsync(params) { billingResult, purchaseToken ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    // Update the appropriate tables/databases to grant user the items
                    Log.d(
                        "TAG_INAPP",
                        " Update the appropriate tables/databases to grant user the items"
                    )
                }
                else -> {
                    Log.w("TAG_INAPP", billingResult.debugMessage)
                }
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase: Purchase in purchases) {
            //if item is purchased
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
                        // Invalid purchase
                        // show error to user
                        toast("Error : Invalid Purchase")
                        return
                    }
                    // else purchase is valid
                    //if item is purchased and not acknowledged
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams, ackPurchase)
                    } else {
                        // TODO CONCEDA PERMISSOES PREMIUM AO USUARIO AQUI
                        // Grant entitlement to the user on item purchase
                        // restart activity
                        if (!purchaseValueFromPref) {
                            savePurchaseValueToPref(true)
                            subscription_status.text = "Item Purchased"
                            itemPurchased = purchase.sku
                            toast(itemPurchased)
                            recreate()
                        } else {
                            subscription_status.text = purchase.sku
                            itemPurchased = purchase.sku
                            toast(purchase.sku)
                        }
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    toast("Purchase is Pending. Please complete Transaction")
                }
                Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                    savePurchaseValueToPref(false)
                    subscription_status.text = "Purchase Status : Not Purchased"

                    toast("Purchase Status Unknown")
                }
            }
        }
    }

    private var ackPurchase: AcknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener { billingResult ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            //if purchase is acknowledged
            // TODO CONCEDA PERMISSOES PREMIUM AO USUARIO AQUI
            // Grant entitlement to the user. and restart activity
            savePurchaseValueToPref(true)
            toast("Item Purchased")
            recreate()
        }
    }

    override fun onClickSubscription(skuDetails: SkuDetails) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        billingClient?.launchBillingFlow(this@InAppPurchaseActivity, billingFlowParams)?.responseCode
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     *
     * Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     *
     */
    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            // To get key go to Developer Console > Select your app > Development Tools > Services & APIs.
            val base64Key = "INSERT YOUR KEY HERE"
            Security.verifyPurchase(base64Key, signedData, signature)
        } catch (e: IOException) {
            false
        }
    }

    private fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private val preferenceObject: SharedPreferences get() = applicationContext.getSharedPreferences(PREF_FILE, 0)
    private val preferenceEditObject: SharedPreferences.Editor
        get() {
            val pref = applicationContext.getSharedPreferences(PREF_FILE, 0)
            return pref.edit()
        }
    private val purchaseValueFromPref: Boolean get() = preferenceObject.getBoolean(PURCHASE_KEY, false)

    private fun savePurchaseValueToPref(value: Boolean) {
        preferenceEditObject.putBoolean(PURCHASE_KEY, value).commit()
    }
}