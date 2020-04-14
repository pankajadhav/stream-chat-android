package io.getstream.chat.android.livedata.usecase

import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.livedata.Call2
import io.getstream.chat.android.livedata.CallImpl2
import io.getstream.chat.android.livedata.ChatRepo
import java.security.InvalidParameterException


class SendMessage(var repo: ChatRepo) {
    operator fun invoke (message: Message): Call2<Message> {
        var runnable = suspend {
            var cid = message.cid
            if (cid.isEmpty()) {
                cid = message.channel.cid
            }
            if (cid.isEmpty()) {
                throw InvalidParameterException("message.cid cant be empty")
            }

            val channelRepo = repo.channel(cid)
            channelRepo.sendMessage(message)
        }
        return CallImpl2<Message>(runnable)
    }
}