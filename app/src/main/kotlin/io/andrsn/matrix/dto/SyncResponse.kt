package io.andrsn.matrix.dto

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

@CompiledJson
data class SyncResponse(
  @field:JsonAttribute(name = "next_batch")
  val nextBatch: String,
  val rooms: SyncRooms,
)

@CompiledJson
data class SyncRooms(
  val join: Map<String, SyncRoom>,
  val invite: Map<String, String> = emptyMap(), // TODO
  val leave: Map<String, String> = emptyMap(), // TODO
)

@CompiledJson
data class SyncRoom(
  val timeline: SyncRoomTimeline,
  val state: SyncRoomState,
)

@CompiledJson
data class SyncRoomTimeline(
  val events: List<SyncRoomEvent>,
  val limited: Boolean,
  @field:JsonAttribute(name = "prev_batch")
  val prevBatch: String?,
)

@CompiledJson
data class SyncRoomState(
  val events: List<SyncRoomEvent>,
)

@CompiledJson
data class SyncRoomEvent(
  @field:JsonAttribute(name = "event_id")
  val eventId: String,
  val type: String,
  val sender: String,
  @field:JsonAttribute(name = "origin_server_ts")
  val originServerTs: Long,
  val content: Map<String, String>,
  @field:JsonAttribute(name = "state_key")
  val stateKey: String,
)
