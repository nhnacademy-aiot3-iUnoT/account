package com.nhnacademy.account.global.error;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
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
    void unexpectedExceptionIsLoggedWithStackTraceAndReturnsGenericError() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts/me");
        IllegalStateException exception = new IllegalStateException("database detail");

        ResponseEntity<ApiResponse<?>> response =
                handler.handleUnexpectedException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().error().code());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), response.getBody().error().message());

        ILoggingEvent loggingEvent = appender.list.getLast();
        assertEquals(Level.ERROR, loggingEvent.getLevel());
        assertEquals(
                "event=unexpected_error exceptionType=IllegalStateException "
                        + "errorCode=INTERNAL_SERVER_ERROR code=G003 httpStatus=500 "
                        + "method=GET path=/api/accounts/me",
                loggingEvent.getFormattedMessage()
        );
        assertNotNull(loggingEvent.getThrowableProxy());
        assertEquals(
                IllegalStateException.class.getName(),
                loggingEvent.getThrowableProxy().getClassName()
        );
    }
}
