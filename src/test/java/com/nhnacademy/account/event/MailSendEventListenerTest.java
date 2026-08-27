package com.nhnacademy.account.event;

import com.nhnacademy.account.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class MailSendEventListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MailSendEventListener listener;

    @Test
    void sendsRequestedMail() {
        MailSendRequestedEvent event = event();

        listener.handle(event);

        then(emailService).should().sendText(
                event.recipient(),
                event.subject(),
                event.content()
        );
    }

    @Test
    void absorbsMailFailure() {
        MailSendRequestedEvent event = event();
        willThrow(new MailSendException("SMTP failure"))
                .given(emailService)
                .sendText(
                        event.recipient(),
                        event.subject(),
                        event.content()
                );

        assertDoesNotThrow(() -> listener.handle(event));
    }

    private MailSendRequestedEvent event() {
        return new MailSendRequestedEvent(
                "test@test.com",
                "subject",
                "content"
        );
    }
}
