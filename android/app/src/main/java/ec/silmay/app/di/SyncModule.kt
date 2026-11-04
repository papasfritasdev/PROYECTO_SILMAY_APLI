package ec.silmay.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ec.silmay.app.BuildConfig
import ec.silmay.app.data.device.DispositivoRepositoryImpl
import ec.silmay.app.data.repository.FirestoreRegistroOperativoRepository
import ec.silmay.app.data.repository.FunctionsAuditoriaRechazosRepository
import ec.silmay.app.data.sync.BitacoraPendientes
import ec.silmay.app.data.sync.BitacoraPendientesPreferencias
import ec.silmay.app.data.sync.ConciliadorSincronizacion
import ec.silmay.app.data.sync.GeneradorUuidV4
import ec.silmay.app.data.sync.RelojSistema
import ec.silmay.app.domain.repository.AuditoriaRechazosRepository
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.repository.DispositivoRepository
import ec.silmay.app.domain.repository.RegistroOperativoRepository
import ec.silmay.app.domain.usecase.GuardarRegistroOperativoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Persistencia local, cola de sincronización y auditoría de rechazos (HT-07). */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    private const val REGION_FUNCTIONS = "southamerica-east1"
    private const val EMULATOR_HOST = "10.0.2.2"
    private const val FUNCTIONS_EMULATOR_PORT = 5001
    private const val PREFS_DISPOSITIVO = "silmay_dispositivo"
    private const val PREFS_SYNC = "silmay_sync"

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        val functions = FirebaseFunctions.getInstance(REGION_FUNCTIONS)
        if (BuildConfig.DEBUG) {
            functions.useEmulator(EMULATOR_HOST, FUNCTIONS_EMULATOR_PORT)
        }
        return functions
    }

    @Provides
    @Singleton
    fun provideDispositivoRepository(@ApplicationContext context: Context): DispositivoRepository =
        DispositivoRepositoryImpl(context.getSharedPreferences(PREFS_DISPOSITIVO, Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun provideBitacoraPendientes(@ApplicationContext context: Context): BitacoraPendientes =
        BitacoraPendientesPreferencias(context.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun provideAuditoriaRechazos(impl: FunctionsAuditoriaRechazosRepository): AuditoriaRechazosRepository = impl

    @Provides
    @Singleton
    fun provideConciliador(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        bitacora: BitacoraPendientes,
        auditoria: AuditoriaRechazosRepository
    ): ConciliadorSincronizacion = ConciliadorSincronizacion(
        firestore = firestore,
        auth = auth,
        bitacora = bitacora,
        auditoria = auditoria,
        alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    @Provides
    @Singleton
    fun provideRegistroOperativoRepository(
        firestore: FirebaseFirestore,
        bitacora: BitacoraPendientes,
        conciliador: ConciliadorSincronizacion
    ): RegistroOperativoRepository = FirestoreRegistroOperativoRepository(firestore, bitacora, conciliador)

    @Provides
    @Singleton
    fun provideGuardarRegistroOperativoUseCase(
        authRepository: AuthRepository,
        dispositivoRepository: DispositivoRepository,
        registroOperativoRepository: RegistroOperativoRepository
    ): GuardarRegistroOperativoUseCase = GuardarRegistroOperativoUseCase(
        authRepository = authRepository,
        dispositivoRepository = dispositivoRepository,
        registroOperativoRepository = registroOperativoRepository,
        generadorUuid = GeneradorUuidV4(),
        reloj = RelojSistema()
    )
}
