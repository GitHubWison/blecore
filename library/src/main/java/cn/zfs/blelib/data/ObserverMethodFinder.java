package cn.zfs.blelib.data;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述:
 * 时间: 2018/5/18 14:42
 * 作者: zengfansheng
 */
class ObserverMethodFinder {
    private static final int BRIDGE = 0x40;
    private static final int SYNTHETIC = 0x1000;
    private static final Map<Class<?>, List<ObserverMethod>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;
    private static final int POOL_SIZE = 4;
    private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];

    List<ObserverMethod> findObserverMethods(Class<?> observerClass) {
        List<ObserverMethod> observerMethods = METHOD_CACHE.get(observerClass);
        if (observerMethods != null) {
            return observerMethods;
        }

        observerMethods = findUsingInfo(observerClass);
        if (observerMethods.isEmpty()) {
            return null;
        } else {
            METHOD_CACHE.put(observerClass, observerMethods);
            return observerMethods;
        }
    }

    private List<ObserverMethod> findUsingInfo(Class<?> observerClass) {
        FindState findState = prepareFindState();
        findState.initForObserver(observerClass);
        while (findState.clazz != null) {
            findState.observerInfo = getObserverInfo(findState);
            if (findState.observerInfo != null) {
                ObserverMethod[] array = findState.observerInfo.getObserverMethods();
                for (ObserverMethod observerMethod : array) {
                    if (findState.checkAdd(observerMethod.method, observerMethod.eventType)) {
                        findState.observberMethods.add(observerMethod);
                    }
                }
            } else {
                findUsingReflectionInSingleClass(findState);
            }
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }

    private List<ObserverMethod> getMethodsAndRelease(FindState findState) {
        List<ObserverMethod> observerMethods = new ArrayList<>(findState.observberMethods);
        findState.recycle();
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState;
                    break;
                }
            }
        }
        return observerMethods;
    }

    private FindState prepareFindState() {
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                FindState state = FIND_STATE_POOL[i];
                if (state != null) {
                    FIND_STATE_POOL[i] = null;
                    return state;
                }
            }
        }
        return new FindState();
    }

    private ObserverInfo getObserverInfo(FindState findState) {
        if (findState.observerInfo != null && findState.observerInfo.getSuperObserverInfo() != null) {
            ObserverInfo superclassInfo = findState.observerInfo.getSuperObserverInfo();
            if (findState.clazz == superclassInfo.getObserverClass()) {
                return superclassInfo;
            }
        }
        return null;
    }

    private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try {
            // This is faster than getMethods, especially when observers are fat classes like Activities
            methods = findState.clazz.getDeclaredMethods();
        } catch (Throwable th) {
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            methods = findState.clazz.getMethods();
            findState.skipSuperClasses = true;
        }
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1) {
                    Observe observeAnnotation = method.getAnnotation(Observe.class);
                    if (observeAnnotation != null) {
                        Class<?> eventType = parameterTypes[0];
                        if (findState.checkAdd(method, eventType)) {
                            ThreadMode threadMode = observeAnnotation.threadMode();
                            findState.observberMethods.add(new ObserverMethod(method, eventType, threadMode));
                        }
                    }
                }
            }
        }
    }

    static void clearCaches() {
        METHOD_CACHE.clear();
    }

    static class FindState {
        final List<ObserverMethod> observberMethods = new ArrayList<>();
        final Map<Class, Object> anyMethodByEventType = new HashMap<>();
        final Map<String, Class> observerClassByMethodKey = new HashMap<>();
        final StringBuilder methodKeyBuilder = new StringBuilder(128);

        Class<?> observerClass;
        Class<?> clazz;
        boolean skipSuperClasses;
        ObserverInfo observerInfo;

        void initForObserver(Class<?> observerClass) {
            this.observerClass = clazz = observerClass;
            skipSuperClasses = false;
            observerInfo = null;
        }

        void recycle() {
            observberMethods.clear();
            anyMethodByEventType.clear();
            observerClassByMethodKey.clear();
            methodKeyBuilder.setLength(0);
            observerClass = null;
            clazz = null;
            skipSuperClasses = false;
            observerInfo = null;
        }

        boolean checkAdd(Method method, Class<?> eventType) {
            // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
            // Usually a observer doesn't have methods listening to the same event type.
            Object existing = anyMethodByEventType.put(eventType, method);
            if (existing == null) {
                return true;
            } else {
                if (existing instanceof Method) {
                    if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                        // Paranoia check
                        throw new IllegalStateException();
                    }
                    // Put any non-Method object to "consume" the existing Method
                    anyMethodByEventType.put(eventType, this);
                }
                return checkAddWithMethodSignature(method, eventType);
            }
        }

        private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
            methodKeyBuilder.setLength(0);
            methodKeyBuilder.append(method.getName());
            methodKeyBuilder.append('>').append(eventType.getName());

            String methodKey = methodKeyBuilder.toString();
            Class<?> methodClass = method.getDeclaringClass();
            Class<?> methodClassOld = observerClassByMethodKey.put(methodKey, methodClass);
            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
                // Only add if not already found in a sub class
                return true;
            } else {
                // Revert the put, old class is further down the class hierarchy
                observerClassByMethodKey.put(methodKey, methodClassOld);
                return false;
            }
        }

        void moveToSuperclass() {
            if (skipSuperClasses) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
                String clazzName = clazz.getName();
                /** Skip system classes, this just degrades performance. */
                if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                    clazz = null;
                }
            }
        }
    }
}
