package me.santio.minehututils.sticky

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.delay
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
        val userId: String,
        var lastMessageId: String? = null,
        var active: Boolean? = false
    )

    private val stickyMessages = ConcurrentHashMap<String, StickyMessage>()

    init {
        startLoop()
    }

    fun start(channelId: String, message: String?, userId: String) {
        val sticky = stickyMessages[channelId]

        if (sticky == null) {
            stickyMessages[channelId] = StickyMessage(
                channelId = channelId,
                message = message ?: error("No previous message found. Please provide a message."),
                userId = userId,
                active = true
            )
        } else {
            message?.let { sticky.message = it }
            sticky.active = true
        }
    }

    fun stop(channelId: String) {
        stickyMessages[channelId]?.active = false
    }

    fun set(channelId: String, message: String, userID: String) {
        val sticky = stickyMessages[channelId]
        if (sticky == null) {
            start(channelId, message, userID)
            stop(channelId)
        } else {
            stickyMessages[channelId]?.message = message
        }
    }

    fun getMessage(channelId: String): String? {
        return stickyMessages[channelId]?.message
    }

    fun isActive(channelId: String): Boolean {
        return stickyMessages[channelId]?.active == true
    }

    fun getEmbed(channelId: String): MessageEmbed {
        val embed = EmbedFactory.default("") {
            it.setTitle(getMessage(channelId))
            it.setFooter("This is an automated sticky message.")
        }.build()

        return embed
    }

    private fun startLoop() {
        scope.launch {
            while (true) {
                delay(5000)

                stickyMessages.values.forEach { sticky ->
                    try {
                        if (!sticky.active!!) return@forEach
                        val channel = bot.getGuildChannelById(sticky.channelId) as? MessageChannel
                            ?: return@forEach

                        val lastMessage = channel.history.retrievePast(1).await().firstOrNull()
                        if (lastMessage?.id == sticky.lastMessageId) return@forEach

                        sticky.lastMessageId?.let {
                            channel.deleteMessageById(it).await()
                        }

                        val embed = channel.sendMessageEmbeds(EmbedFactory.default("") {
                            it.setTitle(sticky.message)
                            it.setFooter("This is an automated sticky message.")
                        }.build()).await()

                        sticky.lastMessageId = embed.id


                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

}
