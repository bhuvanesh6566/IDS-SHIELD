package com.ids.service;

import com.ids.entity.AuditLog;
import com.ids.repository.AuditLogRepository;
import com.ids.repository.UserRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscordBotService extends ListenerAdapter {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private JDA jda;

    @Value("${discord.bot.token}")
    private String botToken;

    @Value("${discord.channel.id}")
    private String channelId;

    public DiscordBotService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        if (botToken == null || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
            System.err.println("Discord Bot Token is not configured. Bot will not start.");
            return;
        }
        try {
            jda = JDABuilder.createDefault(botToken)
                    .addEventListeners(this)
                    .build();
            jda.awaitReady();
            System.out.println("Discord Bot is ready!");
        } catch (Exception e) {
            System.err.println("Failed to start Discord Bot: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void monitorAuditLogs() {
        if (jda == null) return;

        List<AuditLog> pendingLogs = auditLogRepository.findByStatus(AuditLog.VerificationStatus.PENDING);
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            // System.err.println("Discord Channel not found: " + channelId);
            return;
        }

        for (AuditLog log : pendingLogs) {
            sendAlert(channel, log);
            // Mark as PENDING in a way that we don't resend it (or just handle it in the send logic)
            // For demo simplicity, we'll just send and we should ideally have a 'SENT' status
        }
    }

    private void sendAlert(TextChannel channel, AuditLog log) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("⚠️ SECURITY ALERT: Data Change Detected");
        eb.setColor(Color.RED);
        eb.addField("User ID", String.valueOf(log.getUser().getId()), true);
        eb.addField("Field Affected", log.getFieldAffected(), true);
        eb.addField("Action", log.getActionType(), true);
        eb.addField("Old Value", log.getOldValue(), true);
        eb.addField("New Value", log.getNewValue(), true);
        eb.setTimestamp(log.getDetectedAt());
        eb.setFooter("IDS System - Verification Required");

        channel.sendMessageEmbeds(eb.build())
                .addActionRow(
                        Button.success("approve_" + log.getId(), "✅ Approve"),
                        Button.danger("reject_" + log.getId(), "❌ Reject")
                ).queue();
        
        // Update status to prevent resending (but keep PENDING until verified)
        // In a real app, you'd have a 'NOTIFIED' status. 
        // For this demo, let's just mark it as 'PROCESSING' or similar if needed.
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith("approve_") || componentId.startsWith("reject_")) {
            Long logId = Long.parseLong(componentId.split("_")[1]);
            AuditLog log = auditLogRepository.findById(logId).orElse(null);

            if (log == null) {
                event.reply("Log not found!").setEphemeral(true).queue();
                return;
            }

            if (componentId.startsWith("approve_")) {
                handleApproval(log);
                event.reply("✅ Change Approved and Verified.").queue();
            } else {
                handleRejection(log);
                event.reply("❌ Change Rejected. Data Rolled Back.").queue();
            }
            // Remove buttons from the message
            event.getMessage().editMessageComponents().queue();
        }
    }

    private void handleApproval(AuditLog log) {
        log.setStatus(AuditLog.VerificationStatus.APPROVED);
        log.setVerifiedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private void handleRejection(AuditLog log) {
        log.setStatus(AuditLog.VerificationStatus.REJECTED);
        log.setVerifiedAt(LocalDateTime.now());
        auditLogRepository.save(log);

        // Rollback the data in users table
        var user = log.getUser();
        if ("balance".equals(log.getFieldAffected())) {
            // Restore old value
            // In a real app, you'd use a more robust rollback mechanism
            // For demo: UPDATE users SET balance = old_value WHERE id = user_id
            // However, we are in JPA, so:
            user.setBalance(new java.math.BigDecimal(log.getOldValue()));
            userRepository.save(user);
        } else if ("name".equals(log.getFieldAffected())) {
            user.setName(log.getOldValue());
            userRepository.save(user);
        }
    }
}
