package me.santio.minehututils.sticky

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * The stickied manager for handling stickied messages. By design, there can only be one stickied
 * message per channel.
 * @author ddbrother
 */
object StickyManager {
    
    data class StickyMessage(
        val channelId: String,
        val message: String,
        val userId: String,
        var lastMessageId: String? = null
    )

    private val stickyMessages = ConcurrentHashMap<String, StickyMessage>()

    init {
        startLoop()
    }

    /**
     * Sticks the message in a channel when a user attempts to sticky a message
     * @param channelId The channel ID
     * @param message The message to sticky
     * @param userId The ID of the user attempting to sticky a message
     */
    fun stick(channelId: String, message: String, userId: String) {
        stickyMessages[channelId] = StickyMessage(channelId, message, userId)
    }

    /**
     * Unsticks the message in a channel when a user attempts to unstick a message.
     * Since there is only one possible message, channelId is sufficient to identify it.
     * @param channelId The channel ID
     */
    fun unstick(channelId: String) {
        stickyMessages.remove(channelId)
    }

    /**
     * Check to see if a stickied message exists for the given channel.
     * @param channelId The channel ID
     * @return Whether a stickied message exists for the channel
     */
    fun isStickied(channelId: String): Boolean {
        return stickyMessages.containsKey(channelId)
    }

    private fun startLoop() {
        scope.launch {
            while (true) {
                delay(5000)

                stickyMessages.values.forEach { sticky ->
                    try {
                        val channel = bot.getGuildChannelById(sticky.channelId) as? MessageChannel
                            ?: return@forEach

                        val lastMessage = channel.history.retrievePast(1).await().firstOrNull()
                        if (lastMessage?.id == sticky.lastMessageId) return@forEach

                        sticky.lastMessageId?.let {
                            channel.deleteMessageById(it).await()
                        }

                        val message = sticky.message + "\n-# This is an automated sticky message."
                        val newMsg = channel.sendMessage(message).await()
                        sticky.lastMessageId = newMsg.id

                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

}
