package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.Subcommand
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

            addSubcommands(
                Subcommand("start", "Stick the message") {
                    addOptions(
                        Option<String>("message", "The message to stick", required = false)
                    )
                },
                Subcommand("stop", "Unstick the message") {},
                Subcommand("set", "Set the stickied message") {
                    addOptions(
                        Option<String>("message", "The message to stick", required = true)
                    )
                },
                Subcommand("view", "View the stickied message") {
                })
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val channel = event.channel
        val guild = event.guild

        when (event.subcommandName){
            "start" -> {
                val message = event.getOption("message")?.asString
                if (StickyManager.isActive(channel.id)){
                    event.replyEmbeds(
                        EmbedFactory.error(
                            "There is already a stickied message in this channel. Use /sticky set <message> to update it!",
                            guild
                        ).build()
                    ).setEphemeral(true).queue()
                    return
                }
                if (message == null && StickyManager.getMessage(channel.id) == null) {
                    event.replyEmbeds(
                        EmbedFactory.error(
                            "There is not a message to sticky for this channel. Use /sticky start <message> to set a message and start sticking",
                            guild
                        ).build()
                    ).setEphemeral(true).queue()
                    return
                }
                StickyManager.start(channel.id, message, event.user.id)
                event.replyEmbeds(
                    EmbedFactory.success(
                        "Started sticking the message",
                        guild
                    ).build()
                ).setEphemeral(true).queue()
            }

            "stop" -> {
                if (!StickyManager.isActive(channel.id)){
                    event.replyEmbeds(
                        EmbedFactory.error(
                            "There is not a stickied message in this channel.",
                            guild
                        ).build()
                    ).setEphemeral(true).queue()
                    return
                }
                event.replyEmbeds(
                    EmbedFactory.success(
                        "Stopped sticking the message",
                        guild
                    ).build()
                ).setEphemeral(true).queue()

                StickyManager.stop(channel.id)
            }

            "set" -> {
                val message = event.getOption("message")?.asString
                StickyManager.set(channel.id, message!!, event.user.id)
                event.replyEmbeds(
                    EmbedFactory.success(
                        "Updated the stickied message! sending below...",
                        guild
                    ).build(),
                    StickyManager.getEmbed(channel.id)
                ).setEphemeral(true).queue()
            }

            "view" -> {
                if (StickyManager.getMessage(channel.id) == null) {
                    event.replyEmbeds(
                        EmbedFactory.error(
                            "There is not a message to sticky for this channel. Use /sticky start <message> to set a message and start sticking",
                            guild
                        ).build()
                    ).setEphemeral(true).queue()
                    return
                }
                event.replyEmbeds(
                    EmbedFactory.success(
                        "sending below...",
                        guild
                    ).build(),
                    StickyManager.getEmbed(channel.id)
                ).setEphemeral(true).queue()
            }
        }
    }
}
