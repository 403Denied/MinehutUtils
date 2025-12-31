package me.santio.minehututils.sticky

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.MessageEmbed
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
        var message: String,
        var lastMessageId: String? = null,
        var active: Boolean = false
    )

    private val stickyMessages = ConcurrentHashMap<String, StickyMessage>()

    /**
     * Start stickying a message
     * @param channelId The id of the channel to sticky
     * @param message The message to sticky
     */
    fun start(channelId: String, message: String) {
        val sticky = stickyMessages[channelId]

        if (sticky == null) {
            stickyMessages[channelId] = StickyMessage(
                channelId = channelId,
                message = message,
                active = true
            )
        } else {
            message.let { sticky.message = it }
            sticky.active = true
        }
    }

    /**
     * Stop stickying a message
     * @param channelId The id of the channel to sticky
     */
    fun stop(channelId: String) {
        stickyMessages[channelId]?.active = false
    }

    /**
     * Set the sticky message for a channel
     * @param channelId The id of the channel to sticky
     * @param message The message to sticky
     */
    fun set(channelId: String, message: String) {
        val sticky = stickyMessages[channelId]
        if (sticky == null) {
            start(channelId, message)
            stop(channelId)
        } else {
            stickyMessages[channelId]?.message = message
        }
    }

    /**
     * Get the sticky message for a channel
     * @param channelId The id of the channel to sticky
     * @return The message to sticky
     */
    fun getMessage(channelId: String): String? {
        return stickyMessages[channelId]?.message
    }

    /**
     * Get whether a message is being stuck or not
     * @param channelId The id of the channel to sticky
     * @return Whether the message is being stuck or not
     */
    fun isActive(channelId: String): Boolean {
        return stickyMessages[channelId]?.active == true
    }

    /**
     * Get the embed for a stuck message in a channel.
     * @param channelId The id of the channel to sticky
     * @return The embed being stuck for the channel
     */
    fun getEmbed(channelId: String): MessageEmbed {
        if (getMessage(channelId)?.length!! > 256) {
            val embed = EmbedFactory.default(getMessage(channelId)!!) {
                it.setFooter("This is an automated sticky message.")
            }.build()
            return embed
        }
        val embed = EmbedFactory.default("") {
            it.setTitle(getMessage(channelId))
            it.setFooter("This is an automated sticky message.")
        }.build()
        return embed
    }

    /**
     * Refresh the stickied messages
     */
    fun refreshSticky() {
        scope.launch {
            stickyMessages.values.forEach { sticky ->
                try {
                    if (!sticky.active) return@forEach
                    val channel = bot.getGuildChannelById(sticky.channelId) as? MessageChannel
                        ?: return@forEach

                    val lastMessage = channel.history.retrievePast(1).await().firstOrNull()
                    if (lastMessage?.id == sticky.lastMessageId) return@forEach

                    sticky.lastMessageId?.let {
                        channel.deleteMessageById(it).await()
                    }

                    val embed = channel.sendMessageEmbeds(getEmbed(channel.id)).await()

                    sticky.lastMessageId = embed.id

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
