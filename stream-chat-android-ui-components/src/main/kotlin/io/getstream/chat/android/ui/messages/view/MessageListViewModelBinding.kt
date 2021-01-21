@file:JvmName("MessageListViewModelBinding")

package io.getstream.chat.android.ui.messages.view

import androidx.lifecycle.LifecycleOwner
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.AttachmentDownload
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.BlockUser
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.DeleteMessage
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.EndRegionReached
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.FlagMessage
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.GiphyActionSelected
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.LastMessageRead
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.MessageReaction
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.MuteUser
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.ReplyMessage
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.RetryMessage
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Event.ThreadModeEntered
import io.getstream.chat.android.ui.images.AttachmentGalleryActivity

/**
 * Binds [MessageListView] with [MessageListViewModel].
 * Sets the View's handlers and displays new messages based on the ViewModel's state.
 */
@JvmName("bind")
public fun MessageListViewModel.bindView(view: MessageListView, lifecycleOwner: LifecycleOwner) {
    channel.observe(lifecycleOwner) {
        view.init(it, currentUser)
    }
    view.setEndRegionReachedHandler { onEvent(EndRegionReached) }
    view.setLastMessageReadHandler { onEvent(LastMessageRead) }
    view.setOnMessageDeleteHandler { onEvent(DeleteMessage(it)) }
    view.setOnStartThreadHandler { onEvent(ThreadModeEntered(it)) }
    view.setOnMessageFlagHandler { onEvent(FlagMessage(it)) }
    view.setOnSendGiphyHandler { message, giphyAction ->
        onEvent(GiphyActionSelected(message, giphyAction))
    }
    view.setOnMessageRetryHandler { onEvent(RetryMessage(it)) }
    view.setOnMessageReactionHandler { message, reactionType ->
        onEvent(MessageReaction(message, reactionType, enforceUnique = true))
    }
    view.setOnMuteUserHandler { onEvent(MuteUser(it)) }
    view.setOnBlockUserHandler { user, cid -> onEvent(BlockUser(user, cid)) }
    view.setOnReplyMessageHandler { cid, message -> onEvent(ReplyMessage(cid, message)) }
    view.setOnAttachmentDownloadHandler { attachment -> onEvent(AttachmentDownload(attachment)) }

    state.observe(lifecycleOwner) { state ->
        when (state) {
            is MessageListViewModel.State.Loading -> {
                view.hideEmptyStateView()
                view.showLoadingView()
            }
            is MessageListViewModel.State.Result -> {
                if (state.messageListItem.items.isEmpty()) {
                    view.showEmptyStateView()
                } else {
                    view.hideEmptyStateView()
                }
                view.displayNewMessages(state.messageListItem)
                view.hideLoadingView()
            }
        }
    }
    loadMoreLiveData.observe(lifecycleOwner, view::setLoadingMore)
    targetMessage.observe(lifecycleOwner, view::scrollToMessage)


    val attachmentReplyHandler = object: AttachmentGalleryActivity.AttachmentReplyOptionHandler {
        override fun onClick(data: AttachmentGalleryActivity.AttachmentData) {
        }
    }
    view.setOnAttachmentReplyOptionClickHandler(attachmentReplyHandler)
    val attachmentShowInChatHandler = object: AttachmentGalleryActivity.AttachmentShowInChatOptionHandler {
        override fun onClick(data: AttachmentGalleryActivity.AttachmentData) {
            onEvent(MessageListViewModel.Event.ShowMessage(data.messageId))
        }
    }
    view.setOnAttachmentShowInChatOptionClickHandler(attachmentShowInChatHandler)
}
