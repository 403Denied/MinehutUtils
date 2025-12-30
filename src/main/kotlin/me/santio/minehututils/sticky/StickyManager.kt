package me.santio.minehututils.sticky

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.coroutineScope
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

    fun stick(guildId: String, channelId: String, message: String, userId: String) {
        stickyMessages[guildId] = StickyMessage(channelId, message, userId)
    }

    fun unstick(guildId: String) {
        stickyMessages.remove(guildId)
    }
    
    fun isStickied(guildId: String): Boolean {
        return stickyMessages.containsKey(guildId)
    }
    
    fun getStickyChannel(guildId: String): String? {
        return stickyMessages[guildId]?.channelId
    }

    private fun startLoop() {
        scope.launch {
            while (true) {
                delay(5000)
                stickyMessages.values.forEach { sticky ->
                    val channel = bot.getGuildChannelById(sticky.channelId) as? MessageChannel ?: return@forEach

                    if (sticky.lastMessageId != null) {
                        channel.deleteMessageById(sticky.lastMessageId!!).await()
                    }

                    val newMsg = channel.sendMessage(sticky.message).await()
                    sticky.lastMessageId = newMsg.id
                }
            }
        }
    }
}
