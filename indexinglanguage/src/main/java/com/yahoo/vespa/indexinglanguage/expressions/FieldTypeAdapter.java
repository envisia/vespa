// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.indexinglanguage.expressions;

import com.yahoo.document.DataType;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public interface FieldTypeAdapter {

    public DataType getInputType(Expression exp, String fieldName);

    public void tryOutputType(Expression exp, String fieldName, DataType valueType);
}
