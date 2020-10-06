
package org.peidevs.waro.actor;

import java.util.concurrent.atomic.AtomicLong;

class IdGenerator {
    private AtomicLong id = null;

    public IdGenerator(long seed) {
        id = new AtomicLong(seed);
    }

    public long nextId() {
        return id.getAndIncrement();
    }
}
