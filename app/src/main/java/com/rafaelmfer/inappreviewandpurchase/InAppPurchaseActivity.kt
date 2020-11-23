package com.rafaelmfer.inappreviewandpurchase

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
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

//class InAppPurchaseActivity : AppCompatActivity() {
//
//    private var rxBilling: RxBilling? = null
//
//    companion object {
//        fun startScreen(context: Context) {
//            context.startActivity(Intent(context, InAppPurchaseActivity::class.java))
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_in_app_purchase)
//        rxBilling = RxBillingGooglePlayLibrary(this)
//
//
//        rxBilling?.queryInAppPurchases("teste_assinar_premium")?.error
//    }
//
//    private fun handlerInAppProducts(it: InventoryInApp) {
//        product_two.text = "${it.title()} - ${it.price()}"
//
////        product_two.setOnClickListener {
////            rxBilling?.purchase(it.)
////        }
//    }
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        rxBilling!!.destroy()
//    }
//}

class InAppPurchaseActivity : AppCompatActivity(), PurchasesUpdatedListener, ServiceConnection {

    private var skuDetails: SkuDetails? = null
    private var billingClient: BillingClient? = null

    companion object {
        fun startScreen(context: Context) {
            context.startActivity(Intent(context, InAppPurchaseActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_purchase)
        startService()
        setupBillingClient()

        product_one?.setOnClickListener {
            skuDetails?.let {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
                billingClient?.launchBillingFlow(this, billingFlowParams)?.responseCode
            } ?: noSKUMessage()
        }
    }

    private fun startService() {
        val serviceIntent = Intent("com.android.vending.billing.InAppBillingService.BIND")
        serviceIntent.`package` = "com.android.vending"
        bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
    }

    private fun setupBillingClient() {
        billingClient = BillingClient
            .newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        startConnection()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        billingClient?.endConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.v("TAG_INAPP", "Setup Billing Done")
                    // The BillingClient is ready. You can query purchases here.
                    button_load_products_test.setOnClickListener {
                        queryAvailableProducts("android.test.purchased", true)
                    }
                    button_load_products.setOnClickListener {
                        queryAvailableProducts("teste_assinar_premium", false)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.v("TAG_INAPP", "Billing client Disconnected")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun queryAvailableProducts(idProduct: String, test: Boolean) {
        val skuList = ArrayList<String>()
        skuList.add(idProduct)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient?.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                for (skuDetails in skuDetailsList) {
                    Log.v("TAG_INAPP", "skuDetailsList : $skuDetailsList")
                    //This list should contain the products added above
                    updateUI(skuDetails, test)
                }
            } else {
                Toast.makeText(this@InAppPurchaseActivity, "ERROUU  skuDetailsList : $skuDetailsList", Toast.LENGTH_SHORT).show()
                Log.e("TAG_INAPP", "ERROOOOOU")
            }
        }
    }

    private fun updateUI(skuDetails: SkuDetails?, test: Boolean) {
        skuDetails?.let {
            this.skuDetails = it
            if (test) {
                product_one?.text = "${skuDetails.title} - ${skuDetails.price}"
            } else {
                product_two?.text = "${skuDetails.title} - ${skuDetails.price}"
            }
        }
    }

    private fun noSKUMessage() {}

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Toast.makeText(this, "AEEEEE CARAMBA!!", Toast.LENGTH_SHORT).show()
        Log.v("TAG_INAPP", "billingResult responseCode : ${billingResult.responseCode}")

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
//                handleNonConcumablePurchase(purchase)
                handleConsumedPurchases(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
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

    private fun handleNonConcumablePurchase(purchase: Purchase) {
        Log.v("TAG_INAPP", "handlePurchase : ${purchase}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    val billingResponseCode = billingResult.responseCode
                    val billingDebugMessage = billingResult.debugMessage

                    Log.v("TAG_INAPP", "response code: $billingResponseCode")
                    Log.v("TAG_INAPP", "debugMessage : $billingDebugMessage")

                }
            }
        }
    }
}