package ec.silmay.app.data.sync

import ec.silmay.app.domain.sync.GeneradorUuid
import ec.silmay.app.domain.sync.Reloj
import java.util.UUID

/** UUID v4 criptográfico (`SecureRandom`) generado en el cliente (ADR-0005). */
class GeneradorUuidV4 : GeneradorUuid {
    override fun nuevo(): String = UUID.randomUUID().toString()
}

/** Reloj de pared del dispositivo para `createdAtClient`. */
class RelojSistema : Reloj {
    override fun ahoraMillis(): Long = System.currentTimeMillis()
}
