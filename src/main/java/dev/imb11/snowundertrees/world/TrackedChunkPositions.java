package dev.imb11.snowundertrees.world;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

final class TrackedChunkPositions {
    private final LongLinkedOpenHashSet positions = new LongLinkedOpenHashSet();

    void setLoaded(long position, boolean loaded) {
        if (loaded) positions.add(position);
        else positions.remove(position);
    }

    int size() {
        return positions.size();
    }

    long next() {
        long position = positions.firstLong();
        positions.addAndMoveToLast(position);
        return position;
    }
}
