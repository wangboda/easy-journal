package bd.share.journal.exception;

import bd.share.journal.log.LogEntity;

public class NotFormatException extends RuntimeException {

    private LogEntity logEntity;

    public NotFormatException(String message, Throwable cause,LogEntity logEntity) {
        super(message, cause);
        this.logEntity = logEntity;
    }

    public LogEntity getLogEntity() {
        return logEntity;
    }
}
