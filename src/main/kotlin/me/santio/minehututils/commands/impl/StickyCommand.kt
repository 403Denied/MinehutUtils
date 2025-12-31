package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.Subcommand
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.GuildLogger
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
                Subcommand("stop", "Unstick the message"),
                Subcommand("set", "Set the stickied message") {
                    addOptions(
                        Option<String>("message", "The message to stick", required = true)
                    )
                },
                Subcommand("view", "View the stickied message"))
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val channel = event.channel
        val guild = event.guild

        when (event.subcommandName){
            "start" -> {
                val message = event.getOption("message")?.asString
                    ?: StickyManager.getMessage(channel.id)

                if (StickyManager.isActive(channel.id)) error("There is already a stickied message in this channel.")
                if (message == null) error("There is not a message to sticky for this channel. Use /sticky start <message> to set a message and start sticking")
                if (message.length > 4096) error("The message is too long. (max 4096 characters)")

                StickyManager.start(channel.id, message)
                event.replyEmbeds(
                    EmbedFactory.success(
                        "Started sticking the message",
                        guild
                    ).build()
                ).setEphemeral(true).queue()
                GuildLogger.of(guild!!).log(
                    "A sticky was started by ${event.user.asMention}",
                    ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
                    ":package: Channel: ${channel.asMention} *(${channel.name} - ${channel.id})*",
                    ":label: Message: " + StickyManager.getMessage(channel.id)
                ).withContext(event).titled("Sticky Changed").post()
            }

            "stop" -> {
                if (!StickyManager.isActive(channel.id)) error("There is not a stickied message in this channel.")

                event.replyEmbeds(
                    EmbedFactory.success(
                        "Stopped sticking the message",
                        guild
                    ).build()
                ).setEphemeral(true).queue()
                StickyManager.stop(channel.id)
                GuildLogger.of(event.guild!!).log(
                    "A sticky was stopped by ${event.user.asMention}",
                    ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
                    ":package: Channel: ${channel.asMention} *(${channel.name} - ${channel.id})*",
                    ":label: Message: " + StickyManager.getMessage(channel.id)
                ).withContext(event).titled("Sticky Changed").post()
            }

            "set" -> {
                val message = event.getOption("message")?.asString
                    ?: error("You must provide a message to set")
                if (message.length > 4096) error("The message is too long. (max 4096 characters)")

                StickyManager.set(channel.id, message)
                event.replyEmbeds(
                    EmbedFactory.success(
                        "Updated the stickied message! Sending below...",
                        guild
                    ).build(),
                    StickyManager.getEmbed(channel.id)
                ).setEphemeral(true).queue()
                GuildLogger.of(event.guild!!).log(
                    "A sticky messaged was updated by ${event.user.asMention}",
                    ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
                    ":package: Channel: ${channel.asMention} *(${channel.name} - ${channel.id})*",
                    ":label: Message: " + StickyManager.getMessage(channel.id)
                ).withContext(event).titled("Sticky Changed").post()
            }

            "view" -> {
                if (StickyManager.getMessage(channel.id) == null) error("There is not a message to sticky for this channel. Use /sticky start <message> to set a message and start sticking")

                event.replyEmbeds(
                    EmbedFactory.success(
                        "Sending below...",
                        guild
                    ).build(),
                    StickyManager.getEmbed(channel.id)
                ).setEphemeral(true).queue()
            }
        }
    }
}
