package bd.share.journal.log;

public interface JournalWapper {

    void logError(LogEntity entity);

    void logSuccess(LogEntity entity);

    void logWarn(LogEntity entity);

}
