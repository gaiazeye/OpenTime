package com.gaiazeye.businessscheduler.data.remote

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreService {
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
}
