// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.model.container.http.xml;

import com.yahoo.config.model.producer.AbstractConfigProducer;
import com.yahoo.text.XML;
import com.yahoo.vespa.model.builder.xml.dom.DomComponentBuilder;
import com.yahoo.vespa.model.builder.xml.dom.VespaDomBuilder;
import com.yahoo.vespa.model.builder.xml.dom.chains.ChainedComponentModelBuilder;
import com.yahoo.vespa.model.container.http.Filter;
import com.yahoo.vespa.model.container.http.FilterConfigProvider;
import org.w3c.dom.Element;

/**
 * @author tonytv
 * @author gjoranv
 */
public class FilterBuilder extends VespaDomBuilder.DomConfigProducerBuilder<Filter> {

    protected Filter doBuild(AbstractConfigProducer ancestor, Element filterElement) {
        ChainedComponentModelBuilder modelBuilder = new ChainedComponentModelBuilder(filterElement);
        Filter filter =  new Filter(modelBuilder.build());
        DomComponentBuilder.addChildren(ancestor, filterElement, filter);
        addFilterConfig(filterElement, filter);

        return filter;
    }

    private static void addFilterConfig(Element filterElement, Filter filter) {
        Element filterConfigElement = XML.getChild(filterElement, "filter-config");
        if (filterConfigElement == null)
            return;

        FilterConfigProvider filterConfigProvider = filter.addAndInjectConfigProvider();
        putFilterConfig(filterConfigElement, filterConfigProvider);
    }

    private static void putFilterConfig(Element filterConfigElement, FilterConfigProvider filterConfigProvider) {
        for (Element e : XML.getChildren(filterConfigElement)) {
            filterConfigProvider.putConfig(e.getTagName(), XML.getValue(e));
        }
    }

}
