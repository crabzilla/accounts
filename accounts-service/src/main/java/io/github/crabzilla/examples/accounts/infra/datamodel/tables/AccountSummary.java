/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.examples.accounts.infra.datamodel.tables;


import io.github.crabzilla.examples.accounts.infra.datamodel.Keys;
import io.github.crabzilla.examples.accounts.infra.datamodel.Public;
import io.github.crabzilla.examples.accounts.infra.datamodel.tables.records.AccountSummaryRecord;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AccountSummary extends TableImpl<AccountSummaryRecord> {

    private static final long serialVersionUID = -331570780;

    /**
     * The reference instance of <code>public.account_summary</code>
     */
    public static final AccountSummary ACCOUNT_SUMMARY = new AccountSummary();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<AccountSummaryRecord> getRecordType() {
        return AccountSummaryRecord.class;
    }

    /**
     * The column <code>public.account_summary.id</code>.
     */
    public final TableField<AccountSummaryRecord, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.account_summary.balance</code>.
     */
    public final TableField<AccountSummaryRecord, BigDecimal> BALANCE = createField(DSL.name("balance"), org.jooq.impl.SQLDataType.NUMERIC.defaultValue(org.jooq.impl.DSL.field("0.00", org.jooq.impl.SQLDataType.NUMERIC)), this, "");

    /**
     * Create a <code>public.account_summary</code> table reference
     */
    public AccountSummary() {
        this(DSL.name("account_summary"), null);
    }

    /**
     * Create an aliased <code>public.account_summary</code> table reference
     */
    public AccountSummary(String alias) {
        this(DSL.name(alias), ACCOUNT_SUMMARY);
    }

    /**
     * Create an aliased <code>public.account_summary</code> table reference
     */
    public AccountSummary(Name alias) {
        this(alias, ACCOUNT_SUMMARY);
    }

    private AccountSummary(Name alias, Table<AccountSummaryRecord> aliased) {
        this(alias, aliased, null);
    }

    private AccountSummary(Name alias, Table<AccountSummaryRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public <O extends Record> AccountSummary(Table<O> child, ForeignKey<O, AccountSummaryRecord> key) {
        super(child, key, ACCOUNT_SUMMARY);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public UniqueKey<AccountSummaryRecord> getPrimaryKey() {
        return Keys.ACCOUNT_SUMMARY_PKEY;
    }

    @Override
    public List<UniqueKey<AccountSummaryRecord>> getKeys() {
        return Arrays.<UniqueKey<AccountSummaryRecord>>asList(Keys.ACCOUNT_SUMMARY_PKEY);
    }

    @Override
    public AccountSummary as(String alias) {
        return new AccountSummary(DSL.name(alias), this);
    }

    @Override
    public AccountSummary as(Name alias) {
        return new AccountSummary(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public AccountSummary rename(String name) {
        return new AccountSummary(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public AccountSummary rename(Name name) {
        return new AccountSummary(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, BigDecimal> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
