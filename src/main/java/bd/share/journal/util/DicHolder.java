package bd.share.journal.util;

import bd.share.journal.annotation.BindingModel;
import bd.share.journal.annotation.LogAnyway;
import bd.share.journal.annotation.LogIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class DicHolder {

    private static final LocalVariableTableParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    private HashMap<String, List<String>> ignoreDic;
    private HashMap<String, List<String>> logDic;

    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    public DicHolder(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        init();
    }

    public boolean checkRouteExist(String uri){
        return ignoreDic.get(uri) != null;
    }

    public boolean checkLogAnywayExist(String uri){
        return logDic.get(uri) != null && logDic.get(uri).size() > 0;
    }

    // 给定jsonNode
    // 去除logIgnore，逗号表示bm层级
    public void rinseBody(String uri, JsonNode requestJson){
        for (String str : ignoreDic.get(uri)) {
            if(str.contains(",")){
                String[] strings = str.split(",");
                JsonNode temp = requestJson;
                for(int i = 0;i < strings.length - 1;i++){
                    temp = temp.path(strings[i]);
                }
                //防止request的Json字段和field对不上
                if (temp.has(strings[strings.length - 1])){
                    ((ObjectNode)temp).remove(strings[strings.length - 1]);
                }
            }else{
                ((ObjectNode)requestJson).remove(str);
            }
        }
    }

    // 给定jsonNode
    // 去除没有被logAnyway标注的参数
    public JsonNode rinseNotLogAnyway(String uri,JsonNode requestJson){
        List<String> logFields = logDic.get(uri);
        //请求去除没有被 logAnyway 注释的属性
        Iterator<Map.Entry<String, JsonNode>> it = requestJson.fields();
        List<String> removeKey = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            if(!logFields.contains(entry.getKey())){
                removeKey.add(entry.getKey());
            }
        }
        for (String key : removeKey){
            ((ObjectNode)requestJson).remove(key);
        }
        return requestJson;
    }


    private void init() {
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        HashMap<String, List<String>> ignoreMap = new HashMap<>();
        HashMap<String, List<String>> logMap = new HashMap<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> m : map.entrySet()) {

            RequestMappingInfo info = m.getKey();
            HandlerMethod method = m.getValue();
            PatternsRequestCondition p = info.getPatternsCondition();

            String[] paraNames = parameterNameDiscoverer.getParameterNames(method.getMethod());

            for (String url : p.getPatterns()) {
                List<String> ignorePara = new ArrayList<>();
                List<String> logPara = new ArrayList<>();

                //遍历参数 比如get请求
                for (MethodParameter parameter : method.getMethodParameters()) {
                    if (parameter.hasParameterAnnotation(LogIgnore.class)) {
                        int index = parameter.getParameterIndex();
                        ignorePara.add(paraNames[index]);
                    }
                    if (parameter.hasParameterAnnotation(LogAnyway.class)) {
                        if (!parameter.hasParameterAnnotation(LogIgnore.class)) {
                            int index = parameter.getParameterIndex();
                            logPara.add(paraNames[index]);
                        }
                    }
                }

                //遍历类型,为了post请求
                for (Class type : method.getMethod().getParameterTypes()) {
                    if (type.isAnnotationPresent(BindingModel.class)) {
                        getIgnoreAndLogFields(type,ignorePara,logPara,null);
                    }
                }

                ignoreMap.put(url, ignorePara);
                logMap.put(url, logPara);
            }
        }
        ignoreDic = ignoreMap;
        logDic = logMap;
    }

    //递归获取屏蔽和打印的字段数组,多级用,分开
    private void getIgnoreAndLogFields(Class type, List<String> ignoreList, List<String> logList, String superField){
        for (Annotation anno : type.getDeclaredAnnotations()) {

            if (anno.annotationType().equals(BindingModel.class)) {
                List<String> para = Arrays.asList(((BindingModel)anno).ignorePara());
                if (para.size() != 0) {
                    List<String> collect;
                    if (superField != null){
                        collect = para.stream().map(str -> superField + "," +str).collect(Collectors.toList());
                        ignoreList.addAll(collect);
                    }else{
                        ignoreList.addAll(para);
                    }
                }

                for (Field field : type.getDeclaredFields()){
                    //上级决定下级是否打印 isLogAnyway
                    if (((BindingModel) anno).isLogAnyway()) {
                        if (!para.contains(field.getName())) {
                            if (superField != null){
                                logList.add(superField + "," + field.getName());
                            }else{
                                logList.add(field.getName());
                            }
                        }
                    }

                    if (field.getType().isAnnotationPresent(BindingModel.class)) {
                        getIgnoreAndLogFields(field.getType(),ignoreList,logList,field.getName());
                    }
                }
            }
        }
    }

}
