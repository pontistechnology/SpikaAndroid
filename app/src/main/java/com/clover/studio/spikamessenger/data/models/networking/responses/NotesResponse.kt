package com.clover.studio.spikamessenger.data.models.networking.responses

import com.clover.studio.spikamessenger.data.models.entity.Note

data class NotesResponse(
    val status: String,
    val data: NoteData
)

data class NoteData(
    val deleted: Boolean?,
    val note: Note?,
    val notes: List<Note>?
)
