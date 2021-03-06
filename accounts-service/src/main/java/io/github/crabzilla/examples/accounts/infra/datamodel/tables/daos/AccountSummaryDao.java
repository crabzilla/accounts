/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.examples.accounts.infra.datamodel.tables.daos;


import io.github.crabzilla.examples.accounts.infra.datamodel.tables.AccountSummary;
import io.github.crabzilla.examples.accounts.infra.datamodel.tables.records.AccountSummaryRecord;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicQueryExecutor;
import io.github.jklingsporn.vertx.jooq.shared.reactive.AbstractReactiveVertxDAO;
import io.vertx.core.Future;
import org.jooq.Configuration;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AccountSummaryDao extends AbstractReactiveVertxDAO<AccountSummaryRecord, io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary, Integer, Future<List<io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary>>, Future<io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary>, Future<Integer>, Future<Integer>> implements io.github.jklingsporn.vertx.jooq.classic.VertxDAO<AccountSummaryRecord,io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary,Integer> {

    /**
     * @param configuration Used for rendering, so only SQLDialect must be set and must be one of the POSTGREs types.
     * @param delegate A configured AsyncSQLClient that is used for query execution
     */
    public AccountSummaryDao(Configuration configuration, io.vertx.sqlclient.SqlClient delegate) {
        super(AccountSummary.ACCOUNT_SUMMARY, io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary.class, new ReactiveClassicQueryExecutor<AccountSummaryRecord,io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary,Integer>(configuration,delegate,io.github.crabzilla.examples.accounts.infra.datamodel.tables.mappers.RowMappers.getAccountSummaryMapper()));
    }

    @Override
    protected Integer getId(io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary object) {
        return object.getId();
    }

    /**
     * Find records that have <code>balance IN (values)</code> asynchronously
     */
    public Future<List<io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary>> findManyByBalance(Collection<BigDecimal> values) {
        return findManyByCondition(AccountSummary.ACCOUNT_SUMMARY.BALANCE.in(values));
    }

    @Override
    public ReactiveClassicQueryExecutor<AccountSummaryRecord,io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary,Integer> queryExecutor(){
        return (ReactiveClassicQueryExecutor<AccountSummaryRecord,io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary,Integer>) super.queryExecutor();
    }
}
