package ec.silmay.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ec.silmay.app.BuildConfig
import ec.silmay.app.data.firestore.FirestoreProvider
import ec.silmay.app.data.repository.AuthRepositoryImpl
import ec.silmay.app.domain.repository.AuthRepository
import ec.silmay.app.domain.usecase.CerrarSesionUseCase
import ec.silmay.app.domain.usecase.IniciarSesionUseCase
import ec.silmay.app.domain.usecase.ObtenerSesionActualUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val EMULATOR_HOST = "10.0.2.2"
    private const val AUTH_EMULATOR_PORT = 9099

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        val auth = FirebaseAuth.getInstance()
        if (BuildConfig.DEBUG) {
            try {
                auth.useEmulator(EMULATOR_HOST, AUTH_EMULATOR_PORT)
            } catch (_: IllegalStateException) {
                // Emulador ya configurado
            }
        }
        return auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        return FirestoreProvider.configurar(firestore)
    }

    @Provides
    @Singleton
    fun provideIniciarSesionUseCase(authRepository: AuthRepository): IniciarSesionUseCase {
        return IniciarSesionUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideObtenerSesionActualUseCase(authRepository: AuthRepository): ObtenerSesionActualUseCase {
        return ObtenerSesionActualUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideCerrarSesionUseCase(authRepository: AuthRepository): CerrarSesionUseCase {
        return CerrarSesionUseCase(authRepository)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
