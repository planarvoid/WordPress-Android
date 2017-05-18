package com.soundcloud.reporting;

import com.soundcloud.java.collections.Sets;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Metrics are named, multi-dimensional aggregates of {@link DataPoint}s. They can be
 * posted to a {@link ReportingBackend}.
 */
public final class Metric {

    @NotNull private final String name;
    @NotNull private final Set<DataPoint<?>> dataPoints;

    Metric(@NotNull String name, @NotNull Set<DataPoint<?>> dataPoints) {
        this.name = name;
        this.dataPoints = dataPoints;
    }

    public static Metric create(String name, DataPoint<?>... dataPoints) {
        return new Metric(name, Sets.newHashSet(dataPoints));
    }

    public String name() {
        return name;
    }

    public Set<DataPoint<?>> dataPoints() {
        return dataPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Metric that = (Metric) o;
        return MoreObjects.equal(name, that.name) &&
                MoreObjects.equal(dataPoints, that.dataPoints);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(name, dataPoints);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("dataPoints", dataPoints)
                .toString();
    }
}
