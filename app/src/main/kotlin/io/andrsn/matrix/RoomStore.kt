package io.andrsn.matrix

class RoomStore {

  private val rooms = HashMap<String, Room>()

  fun createRoom(room: Room): Room {
    rooms[room.roomId] = room
    return room
  }

  fun getRoom(roomId: String): Room? = rooms[roomId]
}

data class Room(
  val roomId: String,
  val roomName: String,
  val creator: String,
  val createdAt: Long,
)
