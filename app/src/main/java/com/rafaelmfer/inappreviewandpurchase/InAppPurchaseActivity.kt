package com.rafaelmfer.inappreviewandpurchase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class InAppPurchaseActivity : AppCompatActivity() {

    companion object {
        fun startScreen(context: Context) {
            context.startActivity(Intent(context, InAppPurchaseActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_purchase)
    }
}