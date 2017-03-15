/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.parser.sql.dialect.oracle.parser;

import com.dangdang.ddframe.rdb.sharding.parser.sql.context.TableContext;
import com.dangdang.ddframe.rdb.sharding.parser.sql.dialect.oracle.lexer.OracleKeyword;
import com.dangdang.ddframe.rdb.sharding.parser.sql.lexer.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parser.sql.lexer.Symbol;
import com.dangdang.ddframe.rdb.sharding.parser.sql.parser.AbstractSelectParser;
import com.dangdang.ddframe.rdb.sharding.parser.sql.parser.ParserUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parser.sql.parser.SQLExprParser;
import com.dangdang.ddframe.rdb.sharding.parser.visitor.ParseContext;

import java.util.List;

public class OracleSelectParser extends AbstractSelectParser {
    
    public OracleSelectParser(final SQLExprParser exprParser) {
        super(exprParser);
    }
    
    @Override
    protected void customizedSelect() {
        if (getExprParser().equal(DefaultKeyword.FOR)) {
            skipForUpdate();
        }
        if (getSqlContext().getOrderByContexts().isEmpty()) {
            getSqlContext().getOrderByContexts().addAll(getExprParser().parseOrderBy(getSqlContext()));
        }
    }
    
    @Override
    public void query() {
        if (getExprParser().equal(DefaultKeyword.SELECT)) {
            getExprParser().getLexer().nextToken();
            parseDistinct();
            parseSelectList();
        }
        skipInto();
        parseFrom();
        parseWhere();
        skipHierarchicalQueryClause();
        parseGroupBy();
        skipModelClause();
        queryRest();
    }
    
    private void skipInto() {
        if (getExprParser().equal(DefaultKeyword.INTO)) {
            throw new ParserUnsupportedException(getExprParser().getLexer().getToken().getType());
        }
    }
    
    
    private void skipHierarchicalQueryClause() {
        skipConnect();
        skipStart();
        skipConnect();
    }
    
    private void skipStart() {
        if (getExprParser().skipIfEqual(OracleKeyword.START)) {
            getExprParser().accept(DefaultKeyword.WITH);
            getExprParser().parseComparisonCondition(getSqlContext(), new ParseContext(0));
        }
    }
    
    private void skipConnect() {
        if (getExprParser().skipIfEqual(OracleKeyword.CONNECT)) {
            getExprParser().accept(DefaultKeyword.BY);
            getExprParser().skipIfEqual(OracleKeyword.PRIOR);
            if (getExprParser().skipIfEqual(OracleKeyword.NOCYCLE)) {
                getExprParser().skipIfEqual(OracleKeyword.PRIOR);
            }
            getExprParser().parseComparisonCondition(getSqlContext(), new ParseContext(1));
        }
    }
    
    private void skipModelClause() {
        if (!getExprParser().skipIfEqual(OracleKeyword.MODEL)) {
            return;
        }
        skipCellReferenceOptions();
        getExprParser().skipIfEqual(OracleKeyword.RETURN);
        getExprParser().skipIfEqual(DefaultKeyword.ALL);
        getExprParser().skipIfEqual(OracleKeyword.UPDATED);
        getExprParser().skipIfEqual(OracleKeyword.ROWS);
        while (getExprParser().skipIfEqual(OracleKeyword.REFERENCE)) {
            getExprParser().getLexer().nextToken();
            getExprParser().accept(DefaultKeyword.ON);
            getExprParser().skipParentheses();
            skipModelColumnClause();
            skipCellReferenceOptions();
        }
        skipMainModelClause();
    }
    
    private void skipCellReferenceOptions() {
        if (getExprParser().skipIfEqual(OracleKeyword.IGNORE)) {
            getExprParser().accept(OracleKeyword.NAV);
        } else if (getExprParser().skipIfEqual(OracleKeyword.KEEP)) {
            getExprParser().accept(OracleKeyword.NAV);
        }
        if (getExprParser().skipIfEqual(DefaultKeyword.UNIQUE)) {
            getExprParser().skipIfEqual(OracleKeyword.DIMENSION, OracleKeyword.SINGLE);
            getExprParser().skipIfEqual(OracleKeyword.REFERENCE);
        }
    }
    
    private void skipMainModelClause() {
        if (getExprParser().skipIfEqual(OracleKeyword.MAIN)) {
            getExprParser().getLexer().nextToken();
        }
        skipQueryPartitionClause();
        getExprParser().accept(OracleKeyword.DIMENSION);
        getExprParser().accept(DefaultKeyword.BY);
        getExprParser().skipParentheses();
        getExprParser().accept(OracleKeyword.MEASURES);
        getExprParser().skipParentheses();
        skipCellReferenceOptions();
        skipModelRulesClause();
    }

    private void skipModelRulesClause() {
        if (getExprParser().skipIfEqual(OracleKeyword.RULES)) {
            getExprParser().skipIfEqual(DefaultKeyword.UPDATE);
            getExprParser().skipIfEqual(OracleKeyword.UPSERT);
            if (getExprParser().skipIfEqual(OracleKeyword.AUTOMATIC)) {
                getExprParser().accept(DefaultKeyword.ORDER);
            } else if (getExprParser().skipIfEqual(OracleKeyword.SEQUENTIAL)) {
                getExprParser().accept(DefaultKeyword.ORDER);
            }
        }
        if (getExprParser().skipIfEqual(DefaultKeyword.ITERATE)) {
            getExprParser().skipParentheses();
            if (getExprParser().skipIfEqual(DefaultKeyword.UNTIL)) {
                getExprParser().skipParentheses();
            }
        }
        getExprParser().skipParentheses();
    }
    
    private void skipQueryPartitionClause() {
        if (getExprParser().skipIfEqual(OracleKeyword.PARTITION)) {
            getExprParser().accept(DefaultKeyword.BY);
            if (getExprParser().equal(Symbol.LEFT_PAREN)) {
                getExprParser().skipParentheses();
            } else {
                throw new UnsupportedOperationException("Cannot support PARTITION BY without ()");
            }
        }
    }

    private void skipModelColumnClause() {
        throw new ParserUnsupportedException(getExprParser().getLexer().getToken().getType());
    }
    
    @Override
    protected void parseGroupBy() {
        // TODO
//        if (getExprParser().equal(DefaultKeyword.GROUP)) {
//            getExprParser().getLexer().nextToken();
//            getExprParser().accept(DefaultKeyword.BY);
//            while (true) {
//                if (getExprParser().getLexer().identifierEquals("GROUPING")) {
//                    throw new UnsupportedOperationException("Cannot support GROUPING SETS");
//                } 
//                addGroupByItem(getExprParser().expr());
//                if (!getExprParser().equal(Symbol.COMMA)) {
//                    break;
//                }
//                getExprParser().getLexer().nextToken();
//            }
//            if (getExprParser().skipIfEqual(Token.HAVING)) {
//                getExprParser().expr();
//            }
//        } else if (getExprParser().skipIfEqual(Token.HAVING)) {
//            SQLSelectGroupByClause groupBy = new SQLSelectGroupByClause();
//            groupBy.setHaving(getExprParser().expr());
//            if (getExprParser().skipIfEqual(DefaultKeyword.GROUP)) {
//                getExprParser().accept(DefaultKeyword.BY);
//                while (true) {
//                    if (getExprParser().getLexer().identifierEquals("GROUPING")) {
//                        throw new UnsupportedOperationException("Cannot support GROUPING SETS");
//                    }
//                    addGroupByItem(getExprParser().expr());
//                    if (!getExprParser().equal(Symbol.COMMA)) {
//                        break;
//                    }
//                    getExprParser().getLexer().nextToken();
//                }
//            }
//        }
    }
    
    @Override
    public final List<TableContext> parseTable() {
        if (getExprParser().equal(Symbol.LEFT_PAREN)) {
            throw new UnsupportedOperationException("Cannot support subquery");
        }
        if (getExprParser().equal(DefaultKeyword.SELECT)) {
            throw new ParserUnsupportedException(getExprParser().getLexer().getToken().getType());
        }
        if (getExprParser().skipIfEqual(OracleKeyword.ONLY)) {
            getExprParser().skipIfEqual(Symbol.LEFT_PAREN);
            parseQueryTableExpression();
            getExprParser().skipIfEqual(Symbol.RIGHT_PAREN);
            skipFlashbackQueryClause();
        } else {
            parseQueryTableExpression();
            skipPivotClause();
            skipFlashbackQueryClause();
        }
        parseJoinTable();
        return getSqlContext().getTables();
    }
    
    private void parseQueryTableExpression() {
        parseTableFactor();
        parseSample();
        skipPartition();
    }
    
    private void parseSample() {
        if (getExprParser().skipIfEqual(OracleKeyword.SAMPLE)) {
            getExprParser().skipIfEqual(OracleKeyword.BLOCK);
            getExprParser().skipParentheses();
            if (getExprParser().skipIfEqual(OracleKeyword.SEED)) {
                getExprParser().skipParentheses();
            }
        }
    }
    
    private void skipPartition() {
        skipPartition(OracleKeyword.PARTITION);
        skipPartition(OracleKeyword.SUBPARTITION);
    }
    
    private void skipPartition(final OracleKeyword keyword) {
        if (getExprParser().skipIfEqual(keyword)) {
            getExprParser().skipParentheses();
            if (getExprParser().skipIfEqual(DefaultKeyword.FOR)) {
                getExprParser().skipParentheses();
            }
        }
    }
    
    private void skipPivotClause() {
        if (getExprParser().skipIfEqual(OracleKeyword.PIVOT)) {
            getExprParser().skipIfEqual(OracleKeyword.XML);
            getExprParser().skipParentheses();
        } else if (getExprParser().skipIfEqual(OracleKeyword.UNPIVOT)) {
            if (getExprParser().skipIfEqual(OracleKeyword.INCLUDE)) {
                getExprParser().accept(OracleKeyword.NULLS);
            } else if (getExprParser().skipIfEqual(OracleKeyword.EXCLUDE)) {
                getExprParser().accept(OracleKeyword.NULLS);
            }
            getExprParser().skipParentheses();
        }
    }
    
    private void skipFlashbackQueryClause() {
        if (getExprParser().equal(OracleKeyword.VERSIONS)) {
            throw new UnsupportedOperationException("Cannot support Flashback Query");
        } else if (getExprParser().skipIfEqual(DefaultKeyword.AS)) {
            if (getExprParser().skipIfEqual(OracleKeyword.OF)) {
                if (getExprParser().skipIfEqual(OracleKeyword.SCN) || getExprParser().skipIfEqual(OracleKeyword.TIMESTAMP)) {
                    throw new UnsupportedOperationException("Cannot support Flashback Query");
                }
            }
        }
    }
    
    private void skipForUpdate() {
        getExprParser().getLexer().nextToken();
        getExprParser().accept(DefaultKeyword.UPDATE);
        if (getExprParser().skipIfEqual(OracleKeyword.OF)) {
            do {
                getExprParser().parseExpr();
            } while (getExprParser().skipIfEqual(Symbol.COMMA));
        }
        if (getExprParser().equal(OracleKeyword.NOWAIT, OracleKeyword.WAIT)) {
            getExprParser().getLexer().nextToken();
        } else if (getExprParser().skipIfEqual(OracleKeyword.SKIP)) {
            getExprParser().accept(OracleKeyword.LOCKED);
        }
    }
}
