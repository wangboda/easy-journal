package bd.share.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE})
@Documented
public @interface BindingModel {

    boolean isLogAnyway() default false;
    String[] ignorePara() default {};

}