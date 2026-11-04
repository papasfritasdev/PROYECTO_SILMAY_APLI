package ec.silmay.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ec.silmay.app.data.sync.ConciliadorSincronizacion
import javax.inject.Inject

@HiltAndroidApp
class SilmayApp : Application() {

    @Inject
    lateinit var conciliador: ConciliadorSincronizacion

    override fun onCreate() {
        super.onCreate()
        // HT-07: verifica la cola pendiente de sesiones anteriores en cuanto haya sesión.
        conciliador.iniciar()
    }
}
