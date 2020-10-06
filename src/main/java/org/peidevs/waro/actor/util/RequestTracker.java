
package org.peidevs.waro.actor.util;

import java.util.HashMap;
import java.util.Map;

public class RequestTracker {
    private Map<Long, String> requestMap = new HashMap<>();

    public void clear() {
        requestMap.clear();
    }

    public void put(Long requestId, String playerName) {
        requestMap.put(requestId, playerName);
    }

    public void ackReceived(Long requestId) {
        requestMap.remove(requestId);
    }

    public boolean isAllReceived() {
        return requestMap.isEmpty();
    }
}
