// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.grouping.request;

import java.util.Arrays;

/**
 * @author baldersheim
 */
public class ZCurveYFunction extends FunctionNode {
    /**
     * Constructs a new instance of this class.
     *
     * @param exp The expression to evaluate, must evaluate to a long or long[].
     */
    public ZCurveYFunction(GroupingExpression exp) {
        super("zcurve.y", Arrays.asList(exp));
    }
}
