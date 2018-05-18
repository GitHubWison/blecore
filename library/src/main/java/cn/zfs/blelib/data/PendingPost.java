package cn.zfs.blelib.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述:
 * 时间: 2018/5/18 16:14
 * 作者: zengfansheng
 */
class PendingPost {
    private final static List<PendingPost> pendingPostPool = new ArrayList<PendingPost>();

    Object event;
    Observation observation;
    PendingPost next;

    private PendingPost(Object event, Observation observation) {
        this.event = event;
        this.observation = observation;
    }

    static PendingPost obtainPendingPost(Observation observation, Object event) {
        synchronized (pendingPostPool) {
            int size = pendingPostPool.size();
            if (size > 0) {
                PendingPost pendingPost = pendingPostPool.remove(size - 1);
                pendingPost.event = event;
                pendingPost.observation = observation;
                pendingPost.next = null;
                return pendingPost;
            }
        }
        return new PendingPost(event, observation);
    }

    static void releasePendingPost(PendingPost pendingPost) {
        pendingPost.event = null;
        pendingPost.observation = null;
        pendingPost.next = null;
        synchronized (pendingPostPool) {
            if (pendingPostPool.size() < 10000) {
                pendingPostPool.add(pendingPost);
            }
        }
    }
}
