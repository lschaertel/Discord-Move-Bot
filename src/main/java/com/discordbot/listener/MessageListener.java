package com.discordbot.listener;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageListener extends ListenerAdapter {

    private static final String MOVE_PREFIX = "/move ";

    private static final String emoteApprove = "\uD83D\uDC4D";

    @Value ("${service.discord-api-token}")
    private String apiToken;

    private static JDA jda;

    @Bean
    public void initListener() throws LoginException {
        jda = JDABuilder.createDefault(apiToken).build();
        jda.addEventListener(new MessageListener());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        final Message message = event.getMessage();
        final Member member = event.getMember();
        final String messageContent = message.getContentDisplay();
        log.debug("{}: {}", member != null ? member.getEffectiveName() : "[NO MEMBER]", messageContent);

        if (member != null && messageContent.startsWith(MOVE_PREFIX)) {
            if (member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
                message.addReaction(emoteApprove).complete();
            } else {
                event.getChannel().sendMessage(String.format(
                        "To use the '%s' command you need to be in a voice channel. %s", MOVE_PREFIX,
                        event.getMember() )).complete();
            }
        }
        clearHistory();
    }

    private void clearHistory() {
        jda.getTextChannels().forEach(
                textChannel -> textChannel.getHistory().retrievePast(100).complete().forEach(message -> {
                    if (message.getTimeCreated().isBefore(OffsetDateTime.now().minusMinutes(15))
                            && (message.getContentDisplay().startsWith(MOVE_PREFIX)) || message.getAuthor().getId().equals(jda.getSelfUser().getId())) {
                        message.delete().delay(Duration.ofSeconds(10)).complete();
                    }
                })
        );
    }

    @Override
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        List<VoiceChannel> listChannels = jda.getVoiceChannels();
        final Message message = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
        final String messageContent = message.getContentDisplay();

        if (messageContent.startsWith(MOVE_PREFIX) && message.getMember() != null) {
            final String voiceChannel = messageContent.substring(MOVE_PREFIX.length()).trim();
            final Optional<VoiceChannel> channel = checkVoiceChannel(voiceChannel, listChannels);
            channel.ifPresent(value -> processMoveRequest(event.getGuild(), value, message.getMember(), message));
        }
        clearHistory();
    }

    private void processMoveRequest(final Guild guild, final VoiceChannel target, final Member member,
                                    final Message message) {
        final int currentMember = target.getMembers().size();
        final int maxMember = target.getUserLimit();
        final ReactionPaginationAction users = message.retrieveReactionUsers(emoteApprove);
        final List<User> targetMembers = target.getMembers().stream().map(Member::getUser).collect(Collectors.toList());
        final List<User> votedUser = users.getReaction().retrieveUsers().complete();
        targetMembers.retainAll(votedUser);

        if (currentMember < maxMember) {
            guild.moveVoiceMember(member, target).complete();
            message.delete().delay(Duration.ofSeconds(10)).complete();
        } else if (targetMembers.size() >= (maxMember / 2) + 1) {
            guild.moveVoiceMember(member, target).complete();
            message.delete().delay(Duration.ofSeconds(10)).complete();
        }
    }

    private Optional<VoiceChannel> checkVoiceChannel(final String channelName, final List<VoiceChannel> listChannels) {
        return listChannels.stream().filter(channel -> channel.getName().equalsIgnoreCase(channelName)).findFirst();
    }
}
