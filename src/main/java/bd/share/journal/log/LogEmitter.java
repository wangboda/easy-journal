package bd.share.journal.log;

import bd.share.journal.util.DicHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogEmitter implements JournalWapper{

    private static final Logger LOGGER = LoggerFactory.getLogger(LogEmitter.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private DicHolder dicHolder;
    private Boolean logAnyway;

    public LogEmitter(DicHolder dicHolder,Boolean logAnyway) {
        this.dicHolder = dicHolder;
        this.logAnyway = logAnyway;
    }

    @Override
    public void logError(LogEntity entity) {
        LOGGER.error("5 {} {} {} {} {} {} {} {} {}",
                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                "uri:" + entity.getUri(),
                "method:" + entity.getHttpMethod(),
                "statusCode:" + entity.getStatus(),
                "ip:" + entity.getIp(),
                "user:" + entity.getUser(),
                "requestHeader:" + entity.getRequestHeader(),
                "request:" + entity.getParameter(),
                "mistiming:" + entity.getMistiming() + "ms"
        );
    }

    @Override
    public void logSuccess(LogEntity entity) {
        if (!logAnyway){
            try{
                JsonNode node = mapper.readTree(entity.getParameter());
                entity.setParameter(dicHolder.rinseNotLogAnyway(entity.getUri(),node).toString());
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        if (!entity.getParameter().equals("{}")){
            LOGGER.info("1 {} {} {} {} {} {} {} {}",
                    new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                    "uri:" + entity.getUri(),
                    "method:" + entity.getHttpMethod(),
                    "statusCode:" + entity.getStatus(),
                    "ip:" + entity.getIp(),
                    "user:" + entity.getUser(),
                    "request:" + entity.getParameter(),
                    "mistiming:" + entity.getMistiming() + "ms"
            );
        }else{
            LOGGER.info("1 {} {} {} {} {} {} {}",
                    new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                    "uri:" + entity.getUri(),
                    "method:" + entity.getHttpMethod(),
                    "statusCode:" + entity.getStatus(),
                    "ip:" + entity.getIp(),
                    "user:" + entity.getUser(),
                    "mistiming:" + entity.getMistiming() + "ms"
            );
        }
    }

    @Override
    public void logWarn(LogEntity entity) {
        LOGGER.warn("2 {} {} {} {} {} {} {} {} {}",
                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                "uri:" + entity.getUri(),
                "method:" + entity.getHttpMethod(),
                "statusCode:" + entity.getStatus(),
                "ip:" + entity.getIp(),
                "user:" + entity.getUser(),
                "requestHeader:" + entity.getRequestHeader(),
                "request:" + entity.getParameter(),
                "mistiming:" + entity.getMistiming() + "ms"
        );
    }

}
