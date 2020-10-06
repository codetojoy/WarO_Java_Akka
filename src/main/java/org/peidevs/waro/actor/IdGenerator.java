
package org.peidevs.waro.actor;

import java.util.concurrent.atomic.AtomicLong;

class IdGenerator {
    private AtomicLong id = new AtomicLong(100L);

    public long nextId() {
        return id.getAndIncrement();
    }
}
