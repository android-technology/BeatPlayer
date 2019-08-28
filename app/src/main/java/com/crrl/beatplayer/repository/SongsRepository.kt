package com.crrl.beatplayer.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.text.TextUtils
import com.crrl.beatplayer.extensions.toList
import com.crrl.beatplayer.models.Song
import com.crrl.beatplayer.utils.SettingsUtility
import com.crrl.beatplayer.utils.SortModes

interface SongRepository {
    fun loadSongs(): List<Song>
    fun getSongForId(id: Long): Song
    fun searchSongs(searchString: String, limit: Int): List<Song>
}

class SongsRepository() : SongRepository {

    private lateinit var contentResolver: ContentResolver
    private lateinit var settingsUtility: SettingsUtility

    constructor(context: Context?) : this() {
        contentResolver = context!!.contentResolver
        settingsUtility = SettingsUtility.getInstance(context)
    }

    override fun loadSongs(): List<Song> {
        val sl = makeSongCursor(null, null)
            .toList(true) { Song.createFromCursor(this) }
        SortModes.sortSongList(sl, settingsUtility.songSortOrder)
        return sl
    }

    override fun getSongForId(id: Long): Song {
        return Song.createFromCursor(makeSongCursor("_id=$id", null)!!)
    }

    override fun searchSongs(searchString: String, limit: Int): List<Song> {
        val result = makeSongCursor("title LIKE ?", arrayOf("$searchString%"))
            .toList(true) { Song.createFromCursor(this) }
        if (result.size < limit) {
            val moreSongs = makeSongCursor("title LIKE ?", arrayOf("%_$searchString%"))
                .toList(true) { Song.createFromCursor(this) }
            result += moreSongs
        }
        return if (result.size < limit) {
            result
        } else {
            result.subList(0, limit)
        }
    }

    private fun makeSongCursor(
        selection: String?,
        paramArrayOfString: Array<String>?,
        sortOrder: String
    ): Cursor? {
        var selectionStatement = "title != ''"

        if (!TextUtils.isEmpty(selection)) {
            selectionStatement = "$selectionStatement AND $selection"
        }
        return contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                "_id",
                "title",
                "artist",
                "album",
                "duration",
                "track",
                "artist_id",
                "album_id",
                "_data"
            ),
            selectionStatement,
            paramArrayOfString,
            sortOrder
        )
    }

    private fun makeSongCursor(selection: String?, paramArrayOfString: Array<String>?): Cursor? {
        val songSortOrder = settingsUtility.songSortOrder
        return makeSongCursor(selection, paramArrayOfString, songSortOrder)
    }
}