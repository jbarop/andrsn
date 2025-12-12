package io.andrsn.matrix

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RoomStoreTest {

  private lateinit var sut: RoomStore

  @BeforeEach
  fun setup() {
    sut = RoomStore()
  }

  @Test
  fun `should create and retrieve room`() {
    val room = Room(
      roomId = "!testroom:localhost",
      roomName = "Test Room",
      creator = "@user:localhost",
      createdAt = 1000L,
    )

    val createdRoom = sut.createRoom(room)

    assertThat(createdRoom).isEqualTo(room)

    val retrievedRoom = sut.getRoom("!testroom:localhost")

    assertThat(retrievedRoom).isNotNull
    assertThat(retrievedRoom?.roomId).isEqualTo("!testroom:localhost")
    assertThat(retrievedRoom?.roomName).isEqualTo("Test Room")
    assertThat(retrievedRoom?.creator).isEqualTo("@user:localhost")
    assertThat(retrievedRoom?.createdAt).isEqualTo(1000L)
  }

  @Test
  fun `should return null for non-existent room`() {
    val retrievedRoom = sut.getRoom("!nonexistent:localhost")

    assertThat(retrievedRoom).isNull()
  }

  @Test
  fun `should handle multiple rooms`() {
    val room1 = Room(
      roomId = "!room1:localhost",
      roomName = "Room 1",
      creator = "@user:localhost",
      createdAt = 1000L,
    )
    val room2 = Room(
      roomId = "!room2:localhost",
      roomName = "Room 2",
      creator = "@user:localhost",
      createdAt = 2000L,
    )
    val room3 = Room(
      roomId = "!room3:localhost",
      roomName = "Room 3",
      creator = "@user:localhost",
      createdAt = 3000L,
    )

    sut.createRoom(room1)
    sut.createRoom(room2)
    sut.createRoom(room3)

    assertThat(sut.getRoom("!room1:localhost")).isNotNull
    assertThat(sut.getRoom("!room2:localhost")).isNotNull
    assertThat(sut.getRoom("!room3:localhost")).isNotNull
    assertThat(sut.getRoom("!room4:localhost")).isNull()
  }

  @Test
  fun `should retrieve correct room from multiple rooms`() {
    val room1 = Room(
      roomId = "!room1:localhost",
      roomName = "Room 1",
      creator = "@user1:localhost",
      createdAt = 1000L,
    )
    val room2 = Room(
      roomId = "!room2:localhost",
      roomName = "Room 2",
      creator = "@user2:localhost",
      createdAt = 2000L,
    )

    sut.createRoom(room1)
    sut.createRoom(room2)

    val retrieved1 = sut.getRoom("!room1:localhost")
    val retrieved2 = sut.getRoom("!room2:localhost")

    assertThat(retrieved1?.roomName).isEqualTo("Room 1")
    assertThat(retrieved1?.creator).isEqualTo("@user1:localhost")
    assertThat(retrieved1?.createdAt).isEqualTo(1000L)

    assertThat(retrieved2?.roomName).isEqualTo("Room 2")
    assertThat(retrieved2?.creator).isEqualTo("@user2:localhost")
    assertThat(retrieved2?.createdAt).isEqualTo(2000L)
  }
}
