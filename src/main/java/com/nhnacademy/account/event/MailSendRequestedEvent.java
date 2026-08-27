package com.nhnacademy.account.event;

public record MailSendRequestedEvent(
        String recipient,
        String subject,
        String content
) {
}
