package com.rafaelmfer.inappreviewandpurchase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.android.synthetic.main.activity_in_app_review.*

class InAppReviewActivity : AppCompatActivity() {

    lateinit var manager: ReviewManager
    var reviewInfo: ReviewInfo? = null

    companion object {
        fun startScreen(context: Context) {
            context.startActivity(Intent(context, InAppReviewActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_review)

        initReview()
        initSequence()
        button1.visible
    }

    private fun initSequence() {
        button1.setOnClickListener {
            button1.invisible
            button2.visible
        }
        button2.setOnClickListener {
            button2.invisible
            button3.visible
        }
        button3.setOnClickListener {
            button3.invisible
            button4.visible
        }
        button4.setOnClickListener {
            button4.invisible
            askReview()
        }
    }

    private fun initReview() {
        manager = ReviewManagerFactory.create(this@InAppReviewActivity)
        //Para Testes
//        manager = FakeReviewManager(this@MainActivity)

        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { request ->
            if (request.isSuccessful) {
                reviewInfo = request.result
                Log.d("REVIEW_SUCCESS", request.result.toString())
            } else {
                Log.d("REVIEW_ERROR", request.exception.message.toString())
            }
        }
    }

    private fun askReview() {
        if (reviewInfo != null) {
            manager.launchReviewFlow(this@InAppReviewActivity, reviewInfo)
                .addOnFailureListener {
                    Log.d("REVIEW_ERROR", it.message.toString())
                }
                .addOnCompleteListener {
                    Toast.makeText(this, "DEU CERTOOOOOOOOO AEEEE", Toast.LENGTH_SHORT).show()
                    button1.visible
                }
        }
    }
}