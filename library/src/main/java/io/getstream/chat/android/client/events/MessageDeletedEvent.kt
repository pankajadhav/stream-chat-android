package io.getstream.chat.android.client.events

import io.getstream.chat.android.client.models.User

class MessageDeletedEvent : RemoteEvent() {
    lateinit var cid: String
    lateinit var user: User
}