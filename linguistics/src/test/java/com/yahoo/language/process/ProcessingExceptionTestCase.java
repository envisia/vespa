// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.language.process;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen Hult</a>
 */
public class ProcessingExceptionTestCase {

    @Test
    public void requireThatMessageCanBeSet() {
        assertEquals("foo", new ProcessingException("foo").getMessage());
    }

    @Test
    public void requireThatMessageAndCauseCanBeSet() {
        Throwable t = new Throwable();
        ProcessingException e = new ProcessingException("bar", t);
        assertEquals("bar", e.getMessage());
        assertSame(t, e.getCause());
    }

}
