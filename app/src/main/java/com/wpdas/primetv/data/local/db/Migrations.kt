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

    /** 0.0.2 -> 0.0.3: agrupamento estrito (HBO+ ≠ HBO) e groupKey também em filmes; recalcula reimportando. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE movies ADD COLUMN groupKey TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movies_groupKey ON movies (groupKey)")
            db.execSQL("DELETE FROM sync_state")
        }
    }

    /** 0.0.4 -> 0.0.5: liga do time junto do escudo e escudos de competição (cache; zera para buscar de novo com a liga). */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE team_crests ADD COLUMN league TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE team_crests ADD COLUMN leagueId TEXT DEFAULT NULL")
            db.execSQL("DELETE FROM team_crests")
            db.execSQL("CREATE TABLE IF NOT EXISTS `league_badges` (`leagueId` TEXT NOT NULL, `url` TEXT, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`leagueId`))")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
}
