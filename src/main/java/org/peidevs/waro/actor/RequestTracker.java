
package org.peidevs.waro.actor;

import java.util.*;

class RequestTracker {
    private Map<Long, String> requestMap = new HashMap<>();

    protected void clear() {
        requestMap.clear();
    }

    protected void put(Long requestId, String playerName) {
        requestMap.put(requestId, playerName);
    }

    protected void ackReceived(Long requestId) {
        requestMap.remove(requestId);
    }

    protected boolean isAllReceived() {
        return requestMap.isEmpty();
    }
}
