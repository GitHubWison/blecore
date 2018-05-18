package cn.zfs.blelib.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述: 标识观察者的方法
 * 时间: 2018/5/18 14:20
 * 作者: zengfansheng
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Observe {
    ThreadMode threadMode() default ThreadMode.BACKGROUND;
}
