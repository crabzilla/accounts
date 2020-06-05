package io.github.crabzilla.examples.accounts.infra.datamodel.tables.mappers;

import io.vertx.sqlclient.Row;

import java.util.function.Function;

public class RowMappers {

    private RowMappers(){}

    public static Function<Row,io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary> getAccountSummaryMapper() {
        return row -> {
            io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary pojo = new io.github.crabzilla.examples.accounts.infra.datamodel.tables.pojos.AccountSummary();
            pojo.setId(row.getInteger("id"));
            pojo.setBalance(row.getBigDecimal("balance"));
            return pojo;
        };
    }

}
