package com.musicengine.mediapoc.db

import android.database.Cursor
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musicengine.mediapoc.model.TrackKeyNormalizer

/**
 * v1 -> v2
 *  - Re-keys every track-linked row to canonical normalized keys
 *    (TrackKeyNormalizer) so the same song from different apps converges.
 *  - Adds indices for tracks (lastPlayedAt, artist, totalPlays) and a
 *    compound transitions (fromTrackKey, transitionScore) index.
 *
 * Collision policy (two old rows mapping to the same canonical key):
 *  - tracks: keep the row with the highest totalPlays (then totalCompletions)
 *  - transitions: keep the row with the highest transitionCount
 *  - skip_penalties: keep the row with the most recent skipTimestamp
 *  - play_events: rows are re-keyed only; events pointing at a loser migrate
 *    to the surviving track, so no event history is lost
 */
class MigrationV1ToV2 : Migration(1, 2) {

    override fun migrate(db: SupportSQLiteDatabase) {
        // Defer FK constraint checks to commit so we can rewrite parent/child
        // keys in any order without violations (enforcement is re-enabled after).
        db.execSQL("PRAGMA defer_foreign_keys = ON")

        // Re-key event track references (plain columns, not PKs). Fully parseable
        // keys are canonicalized; unparseable FK keys drop only that event row.
        rekeyPlainColumn(db, "play_events", "trackKey", dropRowWhenUnparseable = true)
        // previousTrackKey is optional metadata — null it out rather than dropping history.
        rekeyPlainColumn(db, "play_events", "previousTrackKey", dropRowWhenUnparseable = false)

        val skipPenaltiesDropped = migrateTable(
            db, "skip_penalties", listOf("trackKey"), "skipTimestamp", "skipTimestamp"
        ) { row -> TrackKeyNormalizer.canonicalKeyFromStoredKey(row["trackKey"].orEmpty()) }

        val tracksDropped = migrateTable(
            db, "tracks", listOf("trackKey"), "totalPlays", "totalCompletions"
        ) { row -> TrackKeyNormalizer.canonicalKey(row["title"].orEmpty(), row["artist"].orEmpty()) }

        val transitionsDropped = migrateTable(
            db, "transitions", listOf("fromTrackKey", "toTrackKey"), "transitionCount", "successCount"
        ) { row ->
            val from = TrackKeyNormalizer.canonicalKeyFromStoredKey(row["fromTrackKey"].orEmpty())
            val to = TrackKeyNormalizer.canonicalKeyFromStoredKey(row["toTrackKey"].orEmpty())
            from?.let { f -> to?.let { t -> "from:$f|to:$t" } }
        }

        // New indices introduced with schema version 2
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_lastPlayedAt ON tracks(lastPlayedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_artist ON tracks(artist)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_totalPlays ON tracks(totalPlays)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transitions_fromTrackKey_transitionScore ON transitions(fromTrackKey, transitionScore)")

        Log.d(
            "MigrationV1ToV2",
            "tracksDropped=$tracksDropped transitionsDropped=$transitionsDropped skipPenaltiesDropped=$skipPenaltiesDropped"
        )
    }

    /**
     * Re-keys [keyColumns] of [table] to canonical values using [rowDescriber],
     * resolving collisions deterministically. Loser rows are deleted first, then
     * survivors are stashed to unique temp keys before receiving their final key,
     * which avoids transient primary-key collisions during the rewrite.
     *
     * @return number of rows dropped as duplicates
     */
    private fun migrateTable(
        db: SupportSQLiteDatabase,
        table: String,
        keyColumns: List<String>,
        survivorColumn: String,
        tieBreakColumn: String,
        rowDescriber: (Map<String, String>) -> String?
    ): Int {
        val rows = queryTable(db, table)
        val survivors = LinkedHashMap<String, Pair<Long, Map<String, String>>>()

        for (row in rows) {
            val desc = rowDescriber(row) ?: continue
            if (desc.isEmpty()) continue
            val rowid = (row["rowid"] ?: "0").toLongOrNull() ?: 0L
            val survivorMetric = (row[survivorColumn] ?: "0").toLongOrNull() ?: 0L
            val tieBreakMetric = (row[tieBreakColumn] ?: "0").toLongOrNull() ?: 0L

            val existing = survivors[desc]
            if (existing == null || existing.first == rowid) {
                survivors[desc] = rowid to row // first occurrence wins unless beaten below
                continue
            }
            val existingRow = existing.second
            val existingScore = (existingRow[survivorColumn] ?: "0").toLongOrNull() ?: 0L
            val existingTie = (existingRow[tieBreakColumn] ?: "0").toLongOrNull() ?: 0L
            if (survivorMetric > existingScore ||
                (survivorMetric == existingScore && tieBreakMetric > existingTie)
            ) {
                survivors[desc] = rowid to row
            }
        }

        // 1) Delete losers before stashing so re-keyed survivors never collide with them.
        var dropped = 0
        val keptRowIds = survivors.values.map { it.first }
        for (row in rows) {
            val rowid = (row["rowid"] ?: "0").toLongOrNull() ?: 0L
            if (rowid !in keptRowIds) {
                db.delete(table, "rowid = ?", arrayOf(rowid.toString()))
                dropped++
            }
        }

        // 2) Stash every survivor's keys to unique temp values.
        for ((_, pair) in survivors) {
            val rowid = pair.first
            keyColumns.forEachIndexed { _, column ->
                val temp = "__mig_${column}_$rowid"
                db.execSQL("UPDATE $table SET $column = ? WHERE rowid = ?", arrayOf(temp, rowid.toString()))
            }
        }

        // 3) Restore survivors to their final canonical keys.
        for ((_, pair) in survivors) {
            val rowid = pair.first
            val finalDesc = rowDescriber(pair.second) ?: continue
            val parts = finalDesc.split("|")
            keyColumns.forEachIndexed { index, column ->
                val finalKey = parts[index].substringAfter(':')
                db.execSQL("UPDATE $table SET $column = ? WHERE rowid = ?", arrayOf(finalKey, rowid.toString()))
            }
        }

        return dropped
    }

    /**
     * Re-keys every value in a plain (non-PK) key column of [table].
     * When [dropRowWhenUnparseable] is true, rows with unparseable keys are deleted
     * to preserve FK integrity; otherwise their value is nulled instead.
     */
    private fun rekeyPlainColumn(db: SupportSQLiteDatabase, table: String, column: String, dropRowWhenUnparseable: Boolean) {
        val rows = queryTable(db, table)
        for (row in rows) {
            val key = row[column].orEmpty()
            if (key.isBlank()) continue
            val canonical = TrackKeyNormalizer.canonicalKeyFromStoredKey(key)
            val id = row["id"].orEmpty()
            when {
                canonical != null -> db.execSQL("UPDATE $table SET $column = ? WHERE id = ?", arrayOf(canonical, id))
                dropRowWhenUnparseable -> db.delete(table, "id = ?", arrayOf(id))
                else -> db.execSQL("UPDATE $table SET $column = NULL WHERE id = ?", arrayOf(id))
            }
        }
    }

    private fun queryTable(db: SupportSQLiteDatabase, table: String): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        val cursor = db.query("SELECT rowid, * FROM $table")
        cursor.use { c ->
            val names = c.columnNames
            while (c.moveToNext()) {
                val row = LinkedHashMap<String, String>()
                for (name in names) {
                    val idx = c.getColumnIndex(name)
                    row[name] = if (idx >= 0 && !c.isNull(idx)) c.getString(idx) else ""
                }
                rows.add(row)
            }
        }
        return rows
    }
}