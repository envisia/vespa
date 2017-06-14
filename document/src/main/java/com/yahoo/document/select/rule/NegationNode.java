// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.document.select.rule;

import com.yahoo.document.BucketIdFactory;
import com.yahoo.document.select.BucketSet;
import com.yahoo.document.select.Context;
import com.yahoo.document.select.OrderingSpecification;
import com.yahoo.document.select.Result;
import com.yahoo.document.select.Visitor;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class NegationNode implements ExpressionNode {

    private ExpressionNode node;

    public NegationNode(ExpressionNode node) {
        this.node = node;
    }

    public ExpressionNode getNode() {
        return node;
    }

    public NegationNode setNode(ExpressionNode node) {
        this.node = node;
        return this;
    }

    // Inherit doc from ExpressionNode.
    public BucketSet getBucketSet(BucketIdFactory factory) {
        return null;
    }

    // Inherit doc from ExpressionNode.
    public Object evaluate(Context context) {
        return Result.invert(Result.toResult(node.evaluate(context)));
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "not " + node;
    }

    public OrderingSpecification getOrdering(int order) {
        return null;
    }
}
