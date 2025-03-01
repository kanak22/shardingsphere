/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.binder.statement.dml;

import lombok.Getter;
import org.apache.shardingsphere.infra.binder.segment.table.TablesContext;
import org.apache.shardingsphere.infra.binder.statement.CommonSQLStatementContext;
import org.apache.shardingsphere.infra.binder.type.TableAvailable;
import org.apache.shardingsphere.infra.binder.type.WhereAvailable;
import org.apache.shardingsphere.sql.parser.sql.common.extractor.TableExtractor;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.dml.DeleteStatement;
import org.apache.shardingsphere.sql.parser.sql.common.util.ColumnExtractor;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Delete statement context.
 */
@Getter
public final class DeleteStatementContext extends CommonSQLStatementContext<DeleteStatement> implements TableAvailable, WhereAvailable {
    
    private final TablesContext tablesContext;
    
    private final Collection<WhereSegment> whereSegments = new LinkedList<>();
    
    private final Collection<ColumnSegment> columnSegments = new LinkedList<>();
    
    public DeleteStatementContext(final DeleteStatement sqlStatement) {
        super(sqlStatement);
        tablesContext = new TablesContext(getAllSimpleTableSegments());
        getSqlStatement().getWhere().ifPresent(whereSegments::add);
        ColumnExtractor.extractColumnSegments(columnSegments, whereSegments);
    }
    
    private Collection<SimpleTableSegment> getAllSimpleTableSegments() {
        TableExtractor tableExtractor = new TableExtractor();
        tableExtractor.extractTablesFromDelete(getSqlStatement());
        return filterAliasDeleteTable(tableExtractor.getRewriteTables());
    }
    
    private Collection<SimpleTableSegment> filterAliasDeleteTable(final Collection<SimpleTableSegment> tableSegments) {
        Map<String, SimpleTableSegment> aliasTableSegmentMap = new HashMap<>(tableSegments.size(), 1f);
        for (SimpleTableSegment each : tableSegments) {
            each.getAlias().ifPresent(alias -> aliasTableSegmentMap.putIfAbsent(alias, each));
        }
        Collection<SimpleTableSegment> result = new LinkedList<>();
        for (SimpleTableSegment each : tableSegments) {
            SimpleTableSegment aliasDeleteTable = aliasTableSegmentMap.get(each.getTableName().getIdentifier().getValue());
            if (null == aliasDeleteTable || aliasDeleteTable.equals(each)) {
                result.add(each);
            }
        }
        return result;
    }
    
    @Override
    public Collection<SimpleTableSegment> getAllTables() {
        return tablesContext.getTables();
    }
    
    @Override
    public Collection<WhereSegment> getWhereSegments() {
        return whereSegments;
    }
    
    @Override
    public Collection<ColumnSegment> getColumnSegments() {
        return columnSegments;
    }
}
