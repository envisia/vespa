// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.metrics.simple;

/**
 * The name of the metric and its n-dimensional position. Basically a pair of a
 * Point and a metric name. Written to be robust against null input as the API
 * gives very little guidance, converting null to empty string/point.  Immutable.
 *
 * @author Steinar Knutsen
 */
public class Identifier {

    private final String name;
    private final Point location;

    public Identifier(String name, Point location) {
        this.name = (name == null ? "" : name);
        this.location = (location == null ? Point.emptyPoint() : location);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + location.hashCode();
        result = prime * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        Identifier other = (Identifier) obj;
        if (!location.equals(other.location)) {
            return false;
        }
        if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Identifier [name=").append(name).append(", location=").append(location).append("]");
        return builder.toString();
    }

    public String getName() {
        return name;
    }

    public Point getLocation() {
        return location;
    }

}
