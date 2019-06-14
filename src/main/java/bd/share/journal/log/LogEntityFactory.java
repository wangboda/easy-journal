package bd.share.journal.log;

import bd.share.journal.exception.NotFormatException;
import bd.share.journal.util.ContentConversion;
import bd.share.journal.util.DicHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.UnavailableException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;

public class LogEntityFactory {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static LogEntity getLog(ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper, Throwable throwable,Long startTime,DicHolder dicHolder) throws IOException {
        //构建entity
        LogEntity entity = new LogEntity();
        entity.setUri(requestWrapper.getRequestURI());
        entity.setHttpMethod(requestWrapper.getMethod());
        entity.setStatus(throwable == null ? responseWrapper.getStatusCode() : throwable instanceof UnavailableException ? 503 : 500);
        entity.setIp(getIP(requestWrapper));
        entity.setUser(requestWrapper.getUserPrincipal() != null ? requestWrapper.getUserPrincipal().getName() : "noUser");
        HttpHeaders requestHeaders = new HttpHeaders();
        Enumeration headerNames = requestWrapper.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            requestHeaders.add(headerName, requestWrapper.getHeader(headerName));
        }
        entity.setRequestHeader(objectMapper.writeValueAsString(requestHeaders.toSingleValueMap()));

        //解析请求
        //在这里去除logIgnore
        JsonNode requestJson;
        try{
            requestJson = ContentConversion.convertContentToNode(requestWrapper);
        }catch (NotFormatException e){
            entity.setParameter(e.getMessage());
            entity.setMistiming(System.currentTimeMillis() - startTime);
            responseWrapper.copyBodyToResponse();
            throw new NotFormatException(e.getMessage(),e.getCause(),entity);
        }
        dicHolder.rinseBody(requestWrapper.getRequestURI(),requestJson);
        entity.setParameter(requestJson != null ? requestJson.toString() : "");
        entity.setMistiming(System.currentTimeMillis() - startTime);
        responseWrapper.copyBodyToResponse();
        return entity;
    }


    //获取IP
    private static String getIP(ContentCachingRequestWrapper request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            //当访问localhost时
            if (ip.equals("0:0:0:0:0:0:0:1")) {
                //根据网卡取本机配置的IP
                try {
                    ip = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ip;
    }

}
