/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.examples.accounts.infra.datamodel.tables.records;


import io.github.crabzilla.examples.accounts.infra.datamodel.tables.AccountSummary;
import io.github.crabzilla.examples.accounts.infra.datamodel.tables.interfaces.IAccountSummary;
import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;

import java.math.BigDecimal;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AccountSummaryRecord extends UpdatableRecordImpl<AccountSummaryRecord> implements VertxPojo, Record2<Integer, BigDecimal>, IAccountSummary {

    private static final long serialVersionUID = -1514758680;

    /**
     * Setter for <code>public.account_summary.id</code>.
     */
    @Override
    public AccountSummaryRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>public.account_summary.id</code>.
     */
    @Override
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>public.account_summary.balance</code>.
     */
    @Override
    public AccountSummaryRecord setBalance(BigDecimal value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>public.account_summary.balance</code>.
     */
    @Override
    public BigDecimal getBalance() {
        return (BigDecimal) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, BigDecimal> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Integer, BigDecimal> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return AccountSummary.ACCOUNT_SUMMARY.ID;
    }

    @Override
    public Field<BigDecimal> field2() {
        return AccountSummary.ACCOUNT_SUMMARY.BALANCE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public BigDecimal component2() {
        return getBalance();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public BigDecimal value2() {
        return getBalance();
    }

    @Override
    public AccountSummaryRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public AccountSummaryRecord value2(BigDecimal value) {
        setBalance(value);
        return this;
    }

    @Override
    public AccountSummaryRecord values(Integer value1, BigDecimal value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IAccountSummary from) {
        setId(from.getId());
        setBalance(from.getBalance());
    }

    @Override
    public <E extends IAccountSummary> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AccountSummaryRecord
     */
    public AccountSummaryRecord() {
        super(AccountSummary.ACCOUNT_SUMMARY);
    }

    /**
     * Create a detached, initialised AccountSummaryRecord
     */
    public AccountSummaryRecord(Integer id, BigDecimal balance) {
        super(AccountSummary.ACCOUNT_SUMMARY);

        set(0, id);
        set(1, balance);
    }

    public AccountSummaryRecord(io.vertx.core.json.JsonObject json) {
        this();
        fromJson(json);
    }
}
