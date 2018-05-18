package cn.zfs.blelib.data;

import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.zfs.blelib.exception.BleException;

/**
 * 描述: 蓝牙设备状态、数据被观察者
 * 时间: 2018/4/17 17:02
 * 作者: zengfansheng
 */
public class Observable {
    private final ObserverMethodFinder observerMethodFinder;
    private static final Map<Class<?>, List<Class<?>>> eventTypesCache = new HashMap<>();
    private final Map<Class<?>, CopyOnWriteArrayList<Observation>> observationsByEventType;
    private final Map<Object, List<Class<?>>> typesByObservber;
    private final ExecutorService executorService;
    private final MainThreadSupport mainThreadSupport;
    private final Poster mainThreadPoster;
    private final BackgroundPoster backgroundPoster;
    
    private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };

    private Observable() {
        observerMethodFinder = new ObserverMethodFinder();
        observationsByEventType = new HashMap<>();
        typesByObservber = new HashMap<>();
        executorService = Executors.newCachedThreadPool();
        mainThreadSupport = getMainThreadSupport();
        mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
        backgroundPoster = new BackgroundPoster(this);
    }

    private static class Holder {
        private static final Observable OBSERVABLE = new Observable();
    }
    
    public static Observable getInstance() {
        return Holder.OBSERVABLE;
    }

    ExecutorService getExecutorService() {
        return executorService;
    }
    
    private MainThreadSupport getMainThreadSupport() {
        if (mainThreadSupport != null) {
            return mainThreadSupport;
        } else {
            return new MainThreadSupport.AndroidHandlerMainThreadSupport(Looper.getMainLooper());
        }
    }
    
    public synchronized void clearObserversAndCaches() {
        ObserverMethodFinder.clearCaches();
        eventTypesCache.clear();
        observationsByEventType.clear();
        typesByObservber.clear();
    }
    
    /**
     * 注册观察者
     */
    public void register(Object observer) {
        Class<?> observerClass = observer.getClass();
        List<ObserverMethod> observerMethods = observerMethodFinder.findObserverMethods(observerClass);
        synchronized (this) {
            for (ObserverMethod observerMethod : observerMethods) {
                observe(observer, observerMethod);
            }
        }
    }

    private void observe(Object observer, ObserverMethod observerMethod) {
        Class<?> eventType = observerMethod.eventType;
        Observation newObservation = new Observation(observer, observerMethod);
        CopyOnWriteArrayList<Observation> observations = observationsByEventType.get(eventType);
        if (observations == null) {
            observations = new CopyOnWriteArrayList<>();
            observationsByEventType.put(eventType, observations);
        } else {
            if (observations.contains(newObservation)) {
                throw new BleException("Observer " + observer.getClass() + " already registered to event " + eventType);
            }
        }

        int size = observations.size();
        for (int i = 0; i <= size; i++) {
            if (i == size) {
                observations.add(i, newObservation);
                break;
            }
        }

        List<Class<?>> subscribedEvents = typesByObservber.get(observer);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesByObservber.put(observer, subscribedEvents);
        }
        subscribedEvents.add(eventType);
    }

    private boolean isMainThread() {
        return mainThreadSupport.isMainThread();
    }

    public synchronized boolean isRegistered(Object subscriber) {
        return typesByObservber.containsKey(subscriber);
    }

    private void unsubscribeByEventType(Object observer, Class<?> eventType) {
        List<Observation> observations = observationsByEventType.get(eventType);
        if (observations != null) {
            int size = observations.size();
            for (int i = 0; i < size; i++) {
                Observation observation = observations.get(i);
                if (observation.observer == observer) {
                    observation.active = false;
                    observations.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    public synchronized void unregister(Object observer) {
        List<Class<?>> subscribedTypes = typesByObservber.get(observer);
        if (subscribedTypes != null) {
            for (Class<?> eventType : subscribedTypes) {
                unsubscribeByEventType(observer, eventType);
            }
            typesByObservber.remove(observer);
        }
    }

    public void post(Object event) {
        PostingThreadState postingState = currentPostingThreadState.get();
        List<Object> eventQueue = postingState.eventQueue;
        eventQueue.add(event);

        if (!postingState.isPosting) {
            postingState.isMainThread = isMainThread();
            postingState.isPosting = true;
            if (postingState.canceled) {
                throw new BleException("Internal error. Abort state was not reset");
            }
            try {
                while (!eventQueue.isEmpty()) {
                    postSingleEvent(eventQueue.remove(0), postingState);
                }
            } finally {
                postingState.isPosting = false;
                postingState.isMainThread = false;
            }
        }
    }

    public boolean hasObserverForEvent(Class<?> eventClass) {
        List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
        if (eventTypes != null) {
            int countTypes = eventTypes.size();
            for (int h = 0; h < countTypes; h++) {
                Class<?> clazz = eventTypes.get(h);
                CopyOnWriteArrayList<Observation> observations;
                synchronized (this) {
                    observations = observationsByEventType.get(clazz);
                }
                if (observations != null && !observations.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
        Class<?> eventClass = event.getClass();
        List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
        int countTypes = eventTypes.size();
        for (int h = 0; h < countTypes; h++) {
            Class<?> clazz = eventTypes.get(h);
            postSingleEventForEventType(event, postingState, clazz);
        }
    }

    private void postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        CopyOnWriteArrayList<Observation> observations;
        synchronized (this) {
            observations = observationsByEventType.get(eventClass);
        }
        if (observations != null && !observations.isEmpty()) {
            for (Observation observation : observations) {
                postingState.event = event;
                postingState.observation = observation;
                boolean aborted;
                try {
                    postToObservation(observation, event, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.observation = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
        }
    }

    private void postToObservation(Observation observation, Object event, boolean isMainThread) {
        switch (observation.observerMethod.threadMode) {
            case MAIN:
                if (isMainThread) {
                    invokeObserver(observation, event);
                } else {
                    mainThreadPoster.enqueue(observation, event);
                }
                break;
            case BACKGROUND:
                if (isMainThread) {
                    backgroundPoster.enqueue(observation, event);
                } else {
                    invokeObserver(observation, event);
                }
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + observation.observerMethod.threadMode);
        }
    }

    private static List<Class<?>> lookupAllEventTypes(Class<?> eventClass) {
        synchronized (eventTypesCache) {
            List<Class<?>> eventTypes = eventTypesCache.get(eventClass);
            if (eventTypes == null) {
                eventTypes = new ArrayList<>();
                Class<?> clazz = eventClass;
                while (clazz != null) {
                    eventTypes.add(clazz);
                    addInterfaces(eventTypes, clazz.getInterfaces());
                    clazz = clazz.getSuperclass();
                }
                eventTypesCache.put(eventClass, eventTypes);
            }
            return eventTypes;
        }
    }

    private static void addInterfaces(List<Class<?>> eventTypes, Class<?>[] interfaces) {
        for (Class<?> interfaceClass : interfaces) {
            if (!eventTypes.contains(interfaceClass)) {
                eventTypes.add(interfaceClass);
                addInterfaces(eventTypes, interfaceClass.getInterfaces());
            }
        }
    }

    void invokeObserver(PendingPost pendingPost) {
        Object event = pendingPost.event;
        Observation observation = pendingPost.observation;
        PendingPost.releasePendingPost(pendingPost);
        if (observation.active) {
            invokeObserver(observation, event);
        }
    }

    private void invokeObserver(Observation observation, Object event) {
        try {
            observation.observerMethod.method.invoke(observation.observer, event);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    final static class PostingThreadState {
        final List<Object> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        Observation observation;
        Object event;
        boolean canceled;
    }
}
