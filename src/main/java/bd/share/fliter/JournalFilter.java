package bd.share.fliter;

import bd.share.exception.NotFormatException;
import bd.share.util.ContentConversion;
import bd.share.util.DicHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

public class JournalFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalFilter.class);

    private DicHolder dicHolder;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ThreadLocal<Throwable> cause = new ThreadLocal<>();

    public JournalFilter(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        dicHolder = new DicHolder(requestMappingHandlerMapping);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);

        try{
            filterChain.doFilter(requestWrapper, responseWrapper);
        }catch (Exception e){
            cause.set(e);
            throw e;
        } finally {
            log(startTime,requestWrapper,responseWrapper);
            cause.remove();
        }

    }

    private void log(Long startTime, ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper) throws ServletException, IOException {

        //屏蔽LogIgnore
        String uri = requestWrapper.getRequestURI();
        String httpMethod = requestWrapper.getMethod();
        int statusCode = cause.get() == null ? responseWrapper.getStatusCode() : cause.get() instanceof UnavailableException ? 503 : 500;

        //等于null表示没有这个路由
        if (dicHolder.checkRouteExist(uri)){
            //request
            HttpHeaders requestHeaders = new HttpHeaders();
            Enumeration headerNames = requestWrapper.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                requestHeaders.add(headerName, requestWrapper.getHeader(headerName));
            }

            JsonNode requestJson = null;
            try{
                requestJson = ContentConversion.convertContentToNode(requestWrapper);
            }catch (NotFormatException e){
                LOGGER.error("5 {} {} {} {} {} {} {} {} {}",
                        new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                        "uri:" + uri,
                        "method:" + httpMethod,
                        "statusCode:" + statusCode,
                        "ip:" + getIP(requestWrapper),
                        "user:" + (requestWrapper.getUserPrincipal() != null ? requestWrapper.getUserPrincipal().getName() : "noUser"),
                        "requestHeader:" + objectMapper.writeValueAsString(requestHeaders.toSingleValueMap()),
                        "request:" + e.getMessage(),
                        "mistiming:" + (System.currentTimeMillis() - startTime) + "ms"
                );
                responseWrapper.copyBodyToResponse();
                return;
            }

            dicHolder.rinseBody(requestWrapper.getRequestURI(),requestJson);

            if (cause.get() == null){
                //根据httpCode打印
                if (statusCode > 300 || statusCode < 200) {
                    LOGGER.warn("2 {} {} {} {} {} {} {} {} {}",
                            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                            "uri:" + uri,
                            "method:" + httpMethod,
                            "statusCode:" + statusCode,
                            "ip:" + getIP(requestWrapper),
                            "user:" + (requestWrapper.getUserPrincipal() != null ? requestWrapper.getUserPrincipal().getName() : "noUser"),
                            "requestHeader:" + objectMapper.writeValueAsString(requestHeaders.toSingleValueMap()),
                            "request:" + requestJson.toString(),
                            "mistiming:" + (System.currentTimeMillis() - startTime) + "ms"
                    );
                } else {
                    if (dicHolder.checkLogAnywayExist(uri)){
                        if (httpMethod.equals("GET")){
                            dicHolder.rinseNotLogAnyway(uri,requestJson);
                        }
                        LOGGER.info("1 {} {} {} {} {} {} {} {}",
                                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                                "uri:" + requestWrapper.getRequestURI(),
                                "method:" + httpMethod,
                                "statusCode:" + statusCode,
                                "ip:" + getIP(requestWrapper),
                                "user:" + (requestWrapper.getUserPrincipal() != null ? requestWrapper.getUserPrincipal().getName() : "noUser"),
                                "request:" + requestJson.toString(),
                                "mistiming:" + (System.currentTimeMillis() - startTime) + "ms"
                        );
                    } else {
                        LOGGER.info("1 {} {} {} {} {} {} {}",
                                new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                                "uri:" + requestWrapper.getRequestURI(),
                                "method:" + httpMethod,
                                "statusCode:" + statusCode,
                                "ip:" + getIP(requestWrapper),
                                "user:" + (requestWrapper.getUserPrincipal() != null ? requestWrapper.getUserPrincipal().getName() : "noUser"),
                                "mistiming:" + (System.currentTimeMillis() - startTime) + "ms"
                        );
                    }
                }
            }else{
                LOGGER.error("5 {} {} {} {} {} {} {} {} {}",
                        new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()),
                        "uri:" + uri,
                        "method:" + httpMethod,
                        "statusCode:" + statusCode,
                        "ip:" + getIP(requestWrapper),
                        "user:" + (requestWrapper.getUserPrincipal() != null ? requestWrapper.getUserPrincipal().getName() : "noUser"),
                        "requestHeader:" + objectMapper.writeValueAsString(requestHeaders.toSingleValueMap()),
                        "request:" + requestJson.toString(),
                        "mistiming:" + (System.currentTimeMillis() - startTime) + "ms"
                );
            }
            //避免流被关闭
            responseWrapper.copyBodyToResponse();
        }
    }

    //获取IP
    private String getIP(ContentCachingRequestWrapper request) {
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return super.shouldNotFilter(request);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}

