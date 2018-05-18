package cn.zfs.blelib.data;

/**
 * 描述:
 * 时间: 2018/5/18 15:17
 * 作者: zengfansheng
 */
class Observation {
    final Object observer;
    final ObserverMethod observerMethod;
    volatile boolean active;

    Observation(Object observer, ObserverMethod observerMethod) {
        this.observer = observer;
        this.observerMethod = observerMethod;
        active = true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Observation) {
            Observation otherObservation = (Observation) other;
            return observer == otherObservation.observer
                    && observerMethod.equals(otherObservation.observerMethod);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return observer.hashCode() + observerMethod.methodString.hashCode();
    }
}
