package com.wpdas.primetv.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrações manuais. Regra: atualização do app não pode apagar favoritos, histórico nem
 * a lista importada. Quando uma coluna nova precisa de valor calculado (ex.: groupKey),
 * a migração zera o estado de sincronização para a lista ser reimportada em segundo plano.
 */
object Migrations {
    /** 0.0.1 -> 0.0.2: agrupamento de variantes de qualidade. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE channels ADD COLUMN groupKey TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE channels ADD COLUMN qualityRank INTEGER NOT NULL DEFAULT 4")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_groupKey ON channels (groupKey)")
            db.execSQL("ALTER TABLE movies ADD COLUMN qualityRank INTEGER NOT NULL DEFAULT 4")
            // groupKey/qualityRank precisam ser calculados em Kotlin: força a reimportação do catálogo.
            db.execSQL("DELETE FROM sync_state")
        }
    }

    /** 0.0.2 -> 0.0.3: groupKey passou a distinguir "HBO+" de "HBO"; recalcula reimportando. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM sync_state")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7)
}
