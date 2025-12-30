package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.sticky.StickyManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class StickyCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("sticky", "Sticky a message") {
            setContexts(InteractionContextType.GUILD)

            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

            addOptions(
                Option<Boolean>("stick", "Whether to stick or unstick a message", required = true),
                Option<String>("message", "The message to stick", required = false)
            )
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val stick = event.getOption("stick")?.asBoolean ?: return
        val message = event.getOption("message")?.asString
        val channel = event.channel
        
        if (stick) {
            if (message == null) {
                event.replyEmbeds(EmbedFactory.error("You must provide a message to sticky", event.guild!!).build())
                    .setEphemeral(true)
                    .queue()
                return
            }

            val currentlyStickied = StickyManager.getStickyChannel(event.guild!!.id)
            if (currentlyStickied != null) {
                if (currentlyStickied == channel.id) {
                    event.replyEmbeds(EmbedFactory.error("There is already a message stickied here", event.guild!!).build())
                        .setEphemeral(true)
                        .queue()
                    return
                }
            }
            
            StickyManager.stick(event.guild!!.id, channel.id, message, event.user.id)
            
            event.replyEmbeds(EmbedFactory.success("Successfully stickied message", event.guild!!).build())
                .setEphemeral(true)
                .queue()
        } else {
            if (!StickyManager.isStickied(event.guild!!.id)) {
                event.replyEmbeds(EmbedFactory.error("No message found", event.guild!!).build())
                    .setEphemeral(true)
                    .queue()
                return
            }
            
            StickyManager.unstick(event.guild!!.id)
            
            event.replyEmbeds(EmbedFactory.success("Successfully unstuck message", event.guild!!).build())
                .setEphemeral(true)
                .queue()
        }
    }
}
