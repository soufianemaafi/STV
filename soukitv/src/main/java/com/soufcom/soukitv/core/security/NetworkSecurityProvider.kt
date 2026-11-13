package com.soufcom.soukitv.core.security

import okhttp3.CertificatePinner

/**
 * Fournit la politique TLS appliquée par le client OkHttp partagé.
 *
 * La Phase 1 retourne null : les pins de production seront injectés depuis la
 * configuration de déploiement (cluster.env) lorsque les VPS seront prêts.
 */
interface NetworkSecurityProvider {
    fun certificatePinner(): CertificatePinner?
}

/** Politique réseau actuelle : aucun pin n'est activé. */
object DisabledNetworkSecurityProvider : NetworkSecurityProvider {
    override fun certificatePinner(): CertificatePinner? = null
}
