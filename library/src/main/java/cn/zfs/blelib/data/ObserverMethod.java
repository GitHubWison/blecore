package cn.zfs.blelib.data;

import java.lang.reflect.Method;

/**
 * 描述: 观察者的方法
 * 时间: 2018/5/18 14:37
 * 作者: zengfansheng
 */
class ObserverMethod {
    /**方法*/
    final Method method;
    /**线程模式*/
    final ThreadMode threadMode;
    /**事件类型*/
    final Class<?> eventType;
    String methodString;
    
    ObserverMethod(Method method, Class<?> eventType, ThreadMode threadMode) {
        this.method = method;
        this.eventType = eventType;
        this.threadMode = threadMode;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof ObserverMethod) {
            checkMethodString();
            ObserverMethod otherObserverMethod = (ObserverMethod)other;
            otherObserverMethod.checkMethodString();
            return methodString.equals(otherObserverMethod.methodString);
        } else {
            return false;
        }
    }

    private synchronized void checkMethodString() {
        if (methodString == null) {
            methodString = method.getDeclaringClass().getName() + '#' + method.getName() + '(' + eventType.getName();
        }
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }
}
