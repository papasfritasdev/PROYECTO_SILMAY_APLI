package ec.silmay.app.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import ec.silmay.app.BuildConfig
import kotlinx.coroutines.tasks.await

/**
 * Proveedor y configurador centralizado de Cloud Firestore para Android (M4 / HT-06).
 * Aplica el paradigma Offline-First con caché persistente en disco (RNF-01).
 */
object FirestoreProvider {

    private const val EMULATOR_HOST = "10.0.2.2"
    private const val EMULATOR_PORT = 8080

    fun configurar(firestore: FirebaseFirestore): FirebaseFirestore {
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .build()
            )
            .build()

        firestore.firestoreSettings = settings

        if (BuildConfig.DEBUG) {
            try {
                firestore.useEmulator(EMULATOR_HOST, EMULATOR_PORT)
            } catch (_: IllegalStateException) {
                // Emulador ya configurado en la instancia activa
            }
        }

        return firestore
    }

    suspend fun desactivarRed(firestore: FirebaseFirestore) {
        firestore.disableNetwork().await()
    }

    suspend fun activarRed(firestore: FirebaseFirestore) {
        firestore.enableNetwork().await()
    }
}
