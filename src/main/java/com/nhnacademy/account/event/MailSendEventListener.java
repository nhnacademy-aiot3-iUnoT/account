package com.nhnacademy.account.event;

import com.nhnacademy.account.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailSendEventListener {
    private final EmailService emailService;

    @Async("mailTaskExecutor")
    @EventListener
    public void handle(MailSendRequestedEvent event) {
        try {
            emailService.sendText(
                    event.recipient(),
                    event.subject(),
                    event.content()
            );
        } catch (MailException e) {
            log.error("인증 메일 발송 실패", e);
        }
    }

}
