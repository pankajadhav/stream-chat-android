package io.getstream.chat.android.livedata

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.getstream.chat.android.client.models.Filters
import io.getstream.chat.android.client.utils.SyncStatus
import io.getstream.chat.android.livedata.entity.QueryChannelsEntity
import io.getstream.chat.android.livedata.entity.ReactionEntity
import io.getstream.chat.android.livedata.request.AnyChannelPaginationRequest
import io.getstream.chat.android.livedata.utils.getOrAwaitValue
import kotlinx.coroutines.*
import org.junit.*
import org.junit.runner.RunWith
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
class ChannelRepoInsertTest: BaseTest() {

    @Before
    fun setup() {
        client = createClient()
        setupRepo(client, true)
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
    }

    @After
    fun tearDown() {
        //ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        client.disconnect()
        db.close()


    }

    @Test
    fun sendMessageUseCase() = runBlocking(Dispatchers.IO) {
        val call = repo.useCases.sendMessage(data.message1)
        val result = call.execute()
    }



    @Test
    fun reactionStorage() = runBlocking(Dispatchers.IO) {
        val reactionEntity = ReactionEntity(data.message1.id, data.user1.id, data.reaction1.type)
        reactionEntity.syncStatus = SyncStatus.SYNC_NEEDED
        repo.repos.reactions.insert(reactionEntity)
        val results = repo.retryReactions()
        Truth.assertThat(results.size).isEqualTo(1)
    }

    // TODO: converter/repo test suite

    @Test
    fun sendReaction() = runBlocking(Dispatchers.IO) {

        // TODO: Mock socket and mock client
        // ensure the message exists
        repo._createChannel(data.channel1)
        channelRepo.sendMessage(data.message1)
        repo.setOffline()
        repo.repos.channels.insert(data.channel1)
        channelRepo.upsertMessage(data.message1)
        // send the reaction while offline
        channelRepo.sendReaction(data.reaction1)
        var reactionEntity = repo.repos.reactions.select(data.message1.id, data.user1.id, data.reaction1.type)
        Truth.assertThat(reactionEntity!!.syncStatus).isEqualTo(SyncStatus.SYNC_NEEDED)
        repo.setOnline()
        val reactionEntities = repo.retryReactions()
        Truth.assertThat(reactionEntities.size).isEqualTo(1)
        reactionEntity = repo.repos.reactions.select(data.message1.id, data.user1.id, "like")
        Truth.assertThat(reactionEntity!!.syncStatus).isEqualTo(SyncStatus.SYNCED)

    }

    @Test
    fun deleteReaction() = runBlocking(Dispatchers.IO) {
        repo.setOffline()

        channelRepo.sendReaction(data.reaction1)
        channelRepo.deleteReaction(data.reaction1)

        val reaction = repo.repos.reactions.select(data.message1.id, data.user1.id, data.reaction1.type)
        Truth.assertThat(reaction!!.syncStatus).isEqualTo(SyncStatus.SYNC_NEEDED)
        Truth.assertThat(reaction!!.deletedAt).isNotNull()

        val reactions = repo.retryReactions()
        Truth.assertThat(reactions.size).isEqualTo(1)
    }

    @Test
    @Ignore("Needs a mock, message id already exists")
    fun sendMessage() = runBlocking(Dispatchers.IO){
        // send the message while offline
        repo._createChannel(data.channel1)
        repo.setOffline()
        channelRepo.sendMessage(data.message1)
        // get the message and channel state both live and offline versions
        var roomChannel = repo.repos.channels.select(data.message1.channel.cid)
        var liveChannel = channelRepo.channel.getOrAwaitValue()
        var roomMessages = repo.repos.messages.selectMessagesForChannel(data.message1.channel.cid, AnyChannelPaginationRequest().apply { messageLimit=10 })
        var liveMessages = channelRepo.messages.getOrAwaitValue()

        Truth.assertThat(liveMessages.size).isEqualTo(1)
        Truth.assertThat(liveMessages[0].syncStatus).isEqualTo(SyncStatus.SYNC_NEEDED)
        Truth.assertThat(roomMessages.size).isEqualTo(1)
        Truth.assertThat(roomMessages[0].syncStatus).isEqualTo(SyncStatus.SYNC_NEEDED)
        // verify the message is stored in room, and set to retry
        // verify the channel is updated as well (lastMessage at and lastMessageAt)
        Truth.assertThat(liveChannel.lastMessageAt).isEqualTo(data.message1.createdAt)
        Truth.assertThat(roomChannel!!.lastMessageAt).isEqualTo(data.message1.createdAt)

        var messageEntities = repo.retryMessages()
        Truth.assertThat(messageEntities.size).isEqualTo(1)

        // now we go online and retry, after the retry all state should be updated
        repo.setOnline()
        messageEntities = repo.retryMessages()
        Truth.assertThat(messageEntities.size).isEqualTo(1)

        roomMessages = repo.repos.messages.selectMessagesForChannel(data.message1.channel.cid, AnyChannelPaginationRequest().apply { messageLimit=10 })
        liveMessages = channelRepo.messages.getOrAwaitValue()
        Truth.assertThat(liveMessages[0].syncStatus).isEqualTo(SyncStatus.SYNCED)
        Truth.assertThat(roomMessages[0].syncStatus).isEqualTo(SyncStatus.SYNCED)

    }

    @Test
    fun clean() {
        // clean should remove old typing indicators
        channelRepo.setTyping(data.user1.id, data.user1TypingStartedOld)
        channelRepo.setTyping(data.user2.id, data.user2TypingStarted)

        Truth.assertThat(channelRepo.typing.getOrAwaitValue().size).isEqualTo(2)
        channelRepo.clean()
        Truth.assertThat(channelRepo.typing.getOrAwaitValue().size).isEqualTo(1)
    }

    @Test
    fun insertQuery() = runBlocking(Dispatchers.IO){
        val filter = Filters.eq("type", "messaging")
        val sort = null
        val query = QueryChannelsEntity(filter, sort)
        query.channelCIDs = sortedSetOf("messaging:123", "messaging:234")
        repo.repos.queryChannels.insert(query)
    }

    @Test
    fun insertReaction() = runBlocking(Dispatchers.IO) {
        // check DAO layer and converters
        val reactionEntity = ReactionEntity(data.reaction1)
        repo.repos.reactions.insert(reactionEntity)
        val reactionEntity2 = repo.repos.reactions.select(reactionEntity.messageId, reactionEntity.userId, reactionEntity.type)
        Truth.assertThat(reactionEntity2).isEqualTo(reactionEntity)
        Truth.assertThat(reactionEntity2!!.extraData).isNotNull()
        // verify conversion logic is ok
        val userMap = mutableMapOf(data.user1.id to data.user1)
        val reactionConverted = reactionEntity2!!.toReaction(userMap)
        Truth.assertThat(reactionConverted).isEqualTo(data.reaction1)
    }


    @Test
    fun typing() = runBlocking(Dispatchers.IO){
        // second typing keystroke after each other should not resend the typing event
        Truth.assertThat(channelRepo.keystroke()).isTrue()
        Truth.assertThat(channelRepo.keystroke()).isFalse()
        sleep(3001)
        Truth.assertThat(channelRepo.keystroke()).isTrue()
    }

    @Test
    fun markRead() = runBlocking(Dispatchers.IO){
        // ensure there is at least one message
        channelRepo.upsertMessage(data.message1)
        Truth.assertThat(channelRepo.markRead()).isTrue()
        Truth.assertThat(channelRepo.markRead()).isFalse()

    }


}