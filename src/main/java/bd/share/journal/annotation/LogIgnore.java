package bd.share.journal.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.PARAMETER})
@Documented
public @interface LogIgnore {

}
