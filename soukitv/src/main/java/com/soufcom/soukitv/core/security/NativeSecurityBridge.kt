package com.soufcom.soukitv.core.security

/**
 * Point d'abstraction stable pour les contrôles qui migreront vers JNI/NDK.
 *
 * Aucune bibliothèque native n'est chargée en Phase 1. Les implémentations
 * natives pourront être ajoutées ultérieurement sans modifier les appelants
 * Kotlin ni le contrat de sécurité de l'application.
 */
class NativeSecurityBridge {
    external fun getAuthKey(): String

    external fun verifyIntegrity(): Boolean
}
