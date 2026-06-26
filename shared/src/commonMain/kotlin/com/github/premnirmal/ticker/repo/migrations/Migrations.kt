package com.github.premnirmal.ticker.repo.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Room KMP migrations for [com.github.premnirmal.ticker.repo.QuotesDB].
 *
 * These were ported from the Android-only `SupportSQLiteDatabase` migrations to the multiplatform
 * `androidx.room.migration.Migration` / `androidx.sqlite.SQLiteConnection` API. The raw SQL is
 * unchanged so existing Android databases migrate exactly as before; they now also run on iOS.
 */

/**
 * Migration from version 1 of the database to version 2:
 * Delete field 'description' in table QuoteRow.
 * Add fields 'is_post_market,annual_dividend_rate,annual_dividend_yield' in table QuoteRow.
 * Add table PropertiesRow.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        val tableName = "QuoteRow"
        val tableNameTemp = "new_QuoteRow"

        // Create new QuoteRow table that will replace the table QuoteRow version 1.
        connection.execSQL(
            """
          CREATE TABLE `$tableNameTemp` (
            symbol TEXT NOT NULL, 
            name TEXT NOT NULL, 
            last_trade_price REAL NOT NULL, 
            change_percent REAL NOT NULL, 
            change REAL NOT NULL, 
            exchange TEXT NOT NULL, 
            currency TEXT NOT NULL, 
            is_post_market INTEGER NOT NULL, 
            annual_dividend_rate REAL NOT NULL, 
            annual_dividend_yield REAL NOT NULL, 
            PRIMARY KEY(symbol)
          )
            """.trimIndent()
        )

        // Copy value from table QuoteRow version 1 to table QuoteRow version 2.
        // Use default values is_post_market=0, annual_dividend_rate=0, annual_dividend_yield=0.
        connection.execSQL(
            """
          INSERT INTO `$tableNameTemp` (symbol, name, last_trade_price, change_percent, change, exchange, currency, is_post_market, annual_dividend_rate, annual_dividend_yield)
          SELECT symbol, name, last_trade_price, change_percent, change, exchange, currency, 0, 0, 0 FROM `$tableName`  
            """.trimIndent()
        )

        // Remove table QuoteRow version 1.
        connection.execSQL("DROP TABLE `$tableName`")

        // Rename table QuoteRow version 2 to be the new table QuoteRow.
        connection.execSQL("ALTER TABLE `$tableNameTemp` RENAME TO `$tableName`")

        // Add the new table PropertiesRow.
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `PropertiesRow` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`properties_quote_symbol` TEXT NOT NULL, `notes` TEXT NOT NULL, " +
                "`alert_above` REAL NOT NULL, `alert_below` REAL NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        val tableName = "QuoteRow"
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `dayHigh` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `dayLow` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `previousClose` REAL NOT NULL DEFAULT 0.0;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `open` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `regularMarketVolume` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `peRatio` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekLowChange` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekLowChangePercent` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekHighChange` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekHighChangePercent` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekLow` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekHigh` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `dividendDate` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `earningsDate` REAL;")
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `marketCap` REAL;")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `QuoteRow` ADD COLUMN `isTradeable` INTEGER;")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `QuoteRow` ADD COLUMN `isTriggerable` INTEGER;")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `QuoteRow` ADD COLUMN `marketState` TEXT;")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `QuoteRow` ADD COLUMN `fiftyDayAverage` REAL;")
        connection.execSQL("ALTER TABLE `QuoteRow` ADD COLUMN `twoHundredDayAverage` REAL;")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE PropertiesRow ADD COLUMN displayname TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `FetchLogRow` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`created_at_ms` INTEGER NOT NULL, `source` TEXT NOT NULL, `event` TEXT NOT NULL, `detail` TEXT NOT NULL)"
        )
    }
}

/** The full ordered migration chain, applied by [com.github.premnirmal.ticker.repo.buildQuotesDB]. */
val allMigrations: Array<Migration> = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9
)
