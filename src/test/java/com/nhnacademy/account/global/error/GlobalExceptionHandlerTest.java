package com.nhnacademy.account.global.error;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nhnacademy.account.global.error.exception.ConflictException;
import com.nhnacademy.account.global.util.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();
    private final Logger logger =
            (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void businessErrorLogContainsExceptionTypeAndErrorCode() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/accounts");
        ConflictException exception =
                new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);

        ResponseEntity<ApiResponse<?>> response =
                handler.handleException(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("A002", response.getBody().error().code());

        String logMessage = appender.list.getFirst().getFormattedMessage();
        assertTrue(logMessage.contains("exceptionType=ConflictException"));
        assertTrue(logMessage.contains("errorCode=EMAIL_ALREADY_EXISTS"));
        assertTrue(logMessage.contains("code=A002"));
        assertTrue(logMessage.contains("httpStatus=409"));
    }

}
