package dev.imb11.snowundertrees.world;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

final class TrackedChunkPositions {
    private final LongArrayList positions = new LongArrayList();
    private final LongOpenHashSet loadedPositions = new LongOpenHashSet();
    private int cursor;

    void setLoaded(long position, boolean loaded) {
        if (loaded) {
            if (!loadedPositions.add(position)) return;
            if (cursor == 0) {
                positions.add(position);
            } else {
                positions.add(cursor, position);
                cursor++;
            }
        } else {
            if (!loadedPositions.remove(position)) return;
            int index = positions.indexOf(position);
            positions.removeLong(index);
            if (index < cursor) cursor--;
            if (cursor == positions.size()) cursor = 0;
        }
    }

    int size() {
        return positions.size();
    }

    long next() {
        long position = positions.getLong(cursor++);
        if (cursor == positions.size()) cursor = 0;
        return position;
    }
}
