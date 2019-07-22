package bd.share.journal.fliter;

import bd.share.journal.autoconfig.JournalProperty;
import bd.share.journal.exception.NotFormatException;
import bd.share.journal.log.JournalWapper;
import bd.share.journal.log.LogEntity;
import bd.share.journal.log.LogEntityFactory;
import bd.share.journal.util.DicHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JournalFilter extends OncePerRequestFilter {

    private DicHolder dicHolder;
    private JournalProperty property;
    private JournalWapper wapper;
    private Boolean isEnabled;
    private ThreadLocal<Throwable> cause = new ThreadLocal<>();

    public JournalFilter(DicHolder dicHolder, Boolean isEnabled, JournalWapper wapper) {
        this.dicHolder = dicHolder;
        this.isEnabled = isEnabled;
        this.wapper = wapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = null;
        if (isEnabled){
            long startTime = System.currentTimeMillis();

            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpServletRequest);
            responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);

            try{
                filterChain.doFilter(requestWrapper, responseWrapper);
            }catch (Exception e){
                cause.set(e);
                throw e;
            } finally {
                log(startTime,requestWrapper,responseWrapper);
                cause.remove();
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private void log(Long startTime, ContentCachingRequestWrapper requestWrapper, ContentCachingResponseWrapper responseWrapper) throws ServletException, IOException {
        //判断是否存在路由
        if (dicHolder.checkRouteExist(requestWrapper.getRequestURI())){
            LogEntity entity;
            try{
                entity = LogEntityFactory.getLog(requestWrapper,responseWrapper,cause.get(),startTime,dicHolder);
            }catch (NotFormatException e){
                wapper.logError(e.getLogEntity());
                return;
            }

            int statusCode = entity.getStatus();
            if (cause.get() == null){
                //根据httpCode打印
                if (statusCode > 300 || statusCode < 200) {
                    wapper.logWarn(entity);
                } else {
                    wapper.logSuccess(entity);
                }
            }else{
                wapper.logError(entity);
            }
        }
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

