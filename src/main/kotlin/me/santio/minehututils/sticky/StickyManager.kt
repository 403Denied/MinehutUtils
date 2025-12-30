package me.santio.minehututils.sticky

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.util.concurrent.ConcurrentHashMap

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


    fun stick(channelId: String, message: String, userId: String) {
        stickyMessages[channelId] = StickyMessage(channelId, message, userId)
    }

    fun unstick(channelId: String) {
        stickyMessages.remove(channelId)
    }

    fun isStickied(channelId: String): Boolean {
        return stickyMessages.containsKey(channelId)
    }

    private fun startLoop() {
        scope.launch {
            while (true) {
                delay(5000)
                stickyMessages.values.forEach { sticky ->
                    val channel = bot.getGuildChannelById(sticky.channelId) as? MessageChannel
                        ?: return@forEach

                    val lastMessage = channel.history.retrievePast(1).await().firstOrNull()
                    if (lastMessage?.id == sticky.lastMessageId) return@forEach

                    sticky.lastMessageId?.let { channel.deleteMessageById(it).await() }

                    val newMsg = channel.sendMessage(sticky.message).await()
                    sticky.lastMessageId = newMsg.id
                }
            }
        }
    }
}
