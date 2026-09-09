package com.gaiazeye.businessscheduler

import android.app.Activity
import com.android.billingclient.api.*

class BillingManager(
    private val activity: Activity
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun startConnection(onReady: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    fun launchPurchase(productId: String) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams, object : ProductDetailsResponseListener {
            override fun onProductDetailsResponse(billingResult: BillingResult, result: QueryProductDetailsResult) {
                val productDetailsList = result.productDetailsList
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                    val productDetails = productDetailsList[0]
                    val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)

                    productDetails.subscriptionOfferDetails?.firstOrNull()?.let {
                        productDetailsParamsBuilder.setOfferToken(it.offerToken)
                    }

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
                        .build()

                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
            }
        })
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        // handle subscription purchase result
    }
}
