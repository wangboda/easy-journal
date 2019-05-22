package bd.share.util;

import bd.share.exception.NotFormatException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Enumeration;

public class ContentConversion {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode convertContentToNode(ContentCachingRequestWrapper requestWrapper){
        JsonNode requestJson = objectMapper.createObjectNode();
        String method = requestWrapper.getMethod();
        String contentType = requestWrapper.getHeader(HttpHeaders.CONTENT_TYPE);
        String content = "";

        if (method.equals("POST")){
            if (!StringUtils.isEmpty(contentType)){
                try{
                    switch (contentType){
                        case "application/json":
                            content = IOUtils.toString(requestWrapper.getInputStream(),requestWrapper.getCharacterEncoding());
                            requestJson = objectMapper.readTree(content);
                            return requestJson;
                        case "application/x-www-form-urlencoded":
                            return getParam(requestWrapper);
                        default:
                            content = IOUtils.toString(requestWrapper.getInputStream(),requestWrapper.getCharacterEncoding());
                            ((ObjectNode) requestJson).put(contentType,content);
                            return requestJson;
                    }
                }catch (IOException e){
                    throw new NotFormatException(contentType + ":" + content,e);
                }
            }
        }else{
            return getParam(requestWrapper);
        }
        return requestJson;
    }

    private static JsonNode getParam(ContentCachingRequestWrapper requestWrapper){
        JsonNode requestJson = objectMapper.createObjectNode();
        Enumeration<String> stringEnumeration = requestWrapper.getParameterNames();
        while (stringEnumeration.hasMoreElements()) {
            String paramName = stringEnumeration.nextElement();
            String paramValue = requestWrapper.getParameter(paramName);
            ((ObjectNode) requestJson).put(paramName, paramValue);
        }
        return requestJson;
    }

}
