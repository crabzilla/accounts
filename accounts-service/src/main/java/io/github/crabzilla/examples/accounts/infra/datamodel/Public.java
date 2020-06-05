/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.examples.accounts.infra.datamodel;


import io.github.crabzilla.examples.accounts.infra.datamodel.tables.AccountSummary;
import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = 1540094034;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.account_summary</code>.
     */
    public final AccountSummary ACCOUNT_SUMMARY = AccountSummary.ACCOUNT_SUMMARY;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.<Table<?>>asList(
            AccountSummary.ACCOUNT_SUMMARY);
    }
}
