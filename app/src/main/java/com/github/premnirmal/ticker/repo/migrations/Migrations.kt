package com.github.premnirmal.ticker.repo.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 1 of the database to version 2:
 * Delete field 'description' in table QuoteRow.
 * Add fields 'is_post_market,annual_dividend_rate,annual_dividend_yield' in table QuoteRow.
 * Add table PropertiesRow.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(database: SupportSQLiteDatabase) {
    val TABLE_NAME = "QuoteRow"
    val TABLE_NAME_TEMP = "new_QuoteRow"

    // Create new QuoteRow table that will replace the table QuoteRow version 1.
    database.execSQL(
        """
          CREATE TABLE `${TABLE_NAME_TEMP}` (
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
    database.execSQL(
        """
          INSERT INTO `${TABLE_NAME_TEMP}` (symbol, name, last_trade_price, change_percent, change, exchange, currency, is_post_market, annual_dividend_rate, annual_dividend_yield)
          SELECT symbol, name, last_trade_price, change_percent, change, exchange, currency, 0, 0, 0 FROM `${TABLE_NAME}`  
          """.trimIndent()
    )

    // Remove table QuoteRow version 1.
    database.execSQL("DROP TABLE `${TABLE_NAME}`")

    // Rename table QuoteRow version 2 to be the new table QuoteRow.
    database.execSQL("ALTER TABLE `${TABLE_NAME_TEMP}` RENAME TO `${TABLE_NAME}`")

    // Add the new table PropertiesRow.
    database.execSQL(
        "CREATE TABLE IF NOT EXISTS `PropertiesRow` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `properties_quote_symbol` TEXT NOT NULL, `notes` TEXT NOT NULL, `alert_above` REAL NOT NULL, `alert_below` REAL NOT NULL)"
    )
  }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
  override fun migrate(database: SupportSQLiteDatabase) {
    val tableName = "QuoteRow"
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `dayHigh` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `dayLow` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `previousClose` REAL NOT NULL DEFAULT 0.0;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `open` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `regularMarketVolume` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `peRatio` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekLowChange` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekLowChangePercent` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekHighChange` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekHighChangePercent` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekLow` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `fiftyTwoWeekHigh` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `dividendDate` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `earningsDate` REAL;")
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `marketCap` REAL;")
  }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(database: SupportSQLiteDatabase) {
    val tableName = "QuoteRow"
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `isTradeable` INTEGER;")
  }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
  override fun migrate(database: SupportSQLiteDatabase) {
    val tableName = "QuoteRow"
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `isTriggerable` INTEGER;")
  }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
  override fun migrate(database: SupportSQLiteDatabase) {
    val tableName = "QuoteRow"
    database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `marketState` TEXT;")
  }
}