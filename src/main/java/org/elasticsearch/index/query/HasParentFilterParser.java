/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.search.parent.HasParentFilter;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;

/**
 *
 */
public class HasParentFilterParser implements FilterParser {

    public static final String NAME = "has_parent";

    @Inject
    public HasParentFilterParser() {
    }

    @Override
    public String[] names() {
        return new String[]{NAME, Strings.toCamelCase(NAME)};
    }

    @Override
    public Filter parse(QueryParseContext parseContext) throws IOException, QueryParsingException {
        XContentParser parser = parseContext.parser();

        Query query = null;
        String scope = null;
        String parentType = null;

        String filterName = null;
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                if ("query".equals(currentFieldName)) {
                    query = parseContext.parseInnerQuery();
                } else {
                    throw new QueryParsingException(parseContext.index(), "[has_parent] filter does not support [" + currentFieldName + "]");
                }
            } else if (token.isValue()) {
                if ("type".equals(currentFieldName)) {
                    parentType = parser.text();
                } else if ("_scope".equals(currentFieldName)) {
                    scope = parser.text();
                } else if ("_name".equals(currentFieldName)) {
                    filterName = parser.text();
                } else {
                    throw new QueryParsingException(parseContext.index(), "[has_parent] filter does not support [" + currentFieldName + "]");
                }
            }
        }
        if (query == null) {
            throw new QueryParsingException(parseContext.index(), "[has_parent] filter requires 'query' field");
        }
        if (parentType == null) {
            throw new QueryParsingException(parseContext.index(), "[has_parent] filter requires 'type' field");
        }

        SearchContext searchContext = SearchContext.current();

        HasParentFilter parentFilter = new HasParentFilter(query, scope, parentType, searchContext);
        searchContext.addScopePhase(parentFilter);

        if (filterName != null) {
            parseContext.addNamedFilter(filterName, parentFilter);
        }
        return parentFilter;
    }
}
