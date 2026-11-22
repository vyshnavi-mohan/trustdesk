package com.trustdesk.common.exception;

import com.trustdesk.ticket.TicketNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.trustdesk.knowledge.KnowledgeDocumentNotFoundException;
import com.trustdesk.action.ActionValidationException;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ProblemDetail handleTicketNotFound(
            TicketNotFoundException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(KnowledgeDocumentNotFoundException.class)
    public ProblemDetail handleKnowledgeDocumentNotFound(
            KnowledgeDocumentNotFoundException exception) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }


    @ExceptionHandler(ActionValidationException.class)
    public ProblemDetail handleActionValidationException(
            ActionValidationException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Invalid Action Request");
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(
            IllegalStateException exception) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Action State Conflict");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }
}
