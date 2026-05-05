package com.lauma.client.matcher;

import com.lauma.config.OverrideEntry;

import java.util.List;
import java.util.Optional;

public class MatchPriorityResolver {
    public static Optional<OverrideEntry> resolve(MatchContext ctx, List<OverrideEntry> entries) {
        OverrideEntry best = null;
        int bestPriority = -1;
        for (OverrideEntry entry : entries) {
            if (!ItemMatcher.matches(ctx, entry)) continue;
            int p = priority(entry);
            if (p > bestPriority) {
                bestPriority = p;
                best = entry;
            }
        }
        return Optional.ofNullable(best);
    }

    // Priority: item=1, item+nbt=2, item+cmd=3, item+cmd+nbt=4
    private static int priority(OverrideEntry entry) {
        int p = 1;
        if (entry.hasCustomModelData()) p += 2;
        if (entry.hasNbtCondition()) p += 1;
        return p;
    }
}