package com.example.soukitv.features.home.domain.repository

/** Persistance des IDs de chaînes favorites (contrat pur, sans dépendance Android). */
interface FavoritesStore {
    fun load(): Set<String>
    fun save(ids: Set<String>)
}

/** Implémentation mémoire (tests JVM / valeur par défaut). */
class InMemoryFavoritesStore(initial: Set<String> = emptySet()) : FavoritesStore {
    private var ids: Set<String> = initial
    override fun load(): Set<String> = ids
    override fun save(ids: Set<String>) {
        this.ids = ids
    }
}

