package dev.imb11.snowundertrees.world;

import it.unimi.dsi.fastutil.longs.Long2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import java.util.function.LongConsumer;

final class TrackedChunkPositions {
    private final LongLinkedOpenHashSet positions = new LongLinkedOpenHashSet();
    private final Long2BooleanLinkedOpenHashMap pendingChanges = new Long2BooleanLinkedOpenHashMap();
    private int traversalDepth;

    void setLoaded(long position, boolean loaded) {
        if (traversalDepth != 0) {
            pendingChanges.put(position, loaded);
        } else if (loaded) {
            positions.add(position);
        } else {
            positions.remove(position);
        }
    }

    void forEach(LongConsumer action) {
        traversalDepth++;
        try {
            var iterator = positions.iterator();
            while (iterator.hasNext()) {
                long position = iterator.nextLong();
                if (!pendingChanges.containsKey(position) || pendingChanges.get(position)) {
                    action.accept(position);
                }
            }
        } finally {
            if (--traversalDepth == 0) {
                var iterator = pendingChanges.long2BooleanEntrySet().fastIterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    setLoaded(entry.getLongKey(), entry.getBooleanValue());
                }
                pendingChanges.clear();
            }
        }
    }
}
