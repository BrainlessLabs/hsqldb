/* Copyright (c) 2001-2019, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.hsqldb;

import org.hsqldb.error.Error;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.lib.HsqlDeque;
import org.hsqldb.lib.LongDeque;
import org.hsqldb.persist.PersistentStore;

/**
 * Manages rows involved in transactions
 *
 * @author Fred Toussi (fredt@users dot sourceforge.net)
 * @version 2.5.1
 * @since 2.0.0
 */
public class TransactionManagerMV2PL extends TransactionManagerCommon
implements TransactionManager {

    // functional unit - merged committed transactions
    HsqlDeque committedTransactions          = new HsqlDeque();
    LongDeque committedTransactionTimestamps = new LongDeque();

    public TransactionManagerMV2PL(Database db) {

        super(db);

        lobSession = database.sessionManager.getSysLobSession();
        txModel    = MVLOCKS;
    }

    public long getGlobalChangeTimestamp() {
        return globalChangeTimestamp.get();
    }

    public void setGlobalChangeTimestamp(long ts) {
        globalChangeTimestamp.set(ts);
    }

    public boolean isMVRows() {
        return true;
    }

    public boolean isMVCC() {
        return false;
    }

    public boolean is2PL() {
        return false;
    }

    public int getTransactionControl() {
        return MVLOCKS;
    }

    public void setTransactionControl(Session session, int mode) {
        super.setTransactionControl(session, mode);
    }

    public void completeActions(Session session) {
        endActionTPL(session);
    }

    public boolean prepareCommitActions(Session session) {

        writeLock.lock();

        try {
            int limit = session.rowActionList.size();

            session.actionTimestamp = getNextGlobalChangeTimestamp();

            for (int i = 0; i < limit; i++) {
                RowAction action = (RowAction) session.rowActionList.get(i);

                action.prepareCommit(session);
            }

            return true;
        } finally {
            writeLock.unlock();
        }
    }

    public boolean commitTransaction(Session session) {

        if (session.abortTransaction) {
            return false;
        }

        writeLock.lock();

        try {
            int limit = session.rowActionList.size();

            // new actionTimestamp used for commitTimestamp
            session.actionTimestamp         = getNextGlobalChangeTimestamp();
            session.transactionEndTimestamp = session.actionTimestamp;

            endTransaction(session);

            for (int i = 0; i < limit; i++) {
                RowAction action = (RowAction) session.rowActionList.get(i);

                action.commit(session);
            }

            adjustLobUsage(session);
            persistCommit(session);

            int newLimit = session.rowActionList.size();

            if (newLimit > limit) {
                Object[] list = session.rowActionList.getArray();

                mergeTransaction(list, limit, newLimit,
                                 session.actionTimestamp);
                finaliseRows(session, list, limit, newLimit);
                session.rowActionList.setSize(limit);
            }

            // session.actionTimestamp is the committed tx timestamp
            if (session == lobSession
                    || getFirstLiveTransactionTimestamp()
                       > session.actionTimestamp) {
                Object[] list = session.rowActionList.getArray();

                mergeTransaction(list, 0, limit, session.actionTimestamp);
                finaliseRows(session, list, 0, limit);
            } else {
                Object[] list = session.rowActionList.toArray();

                addToCommittedQueue(session, list);
            }

            session.isTransaction = false;

            endTransactionTPL(session);
        } finally {
            writeLock.unlock();
            session.actionSet.clear();
        }

        return true;
    }

    public void rollback(Session session) {

        writeLock.lock();

        try {
            session.abortTransaction        = false;
            session.actionTimestamp         = getNextGlobalChangeTimestamp();
            session.transactionEndTimestamp = session.actionTimestamp;

            rollbackPartial(session, 0, session.transactionTimestamp);
            endTransaction(session);
            session.logSequences();

            session.isTransaction = false;

            endTransactionTPL(session);
        } finally {
            writeLock.unlock();
        }
    }

    public void rollbackSavepoint(Session session, int index) {

        long timestamp = session.sessionContext.savepointTimestamps.get(index);
        Integer oi = (Integer) session.sessionContext.savepoints.get(index);
        int     start  = oi.intValue();

        while (session.sessionContext.savepoints.size() > index + 1) {
            session.sessionContext.savepoints.remove(
                session.sessionContext.savepoints.size() - 1);
            session.sessionContext.savepointTimestamps.removeLast();
        }

        rollbackPartial(session, start, timestamp);
    }

    public void rollbackAction(Session session) {

        rollbackPartial(session, session.actionIndex,
                        session.actionStartTimestamp);
        endActionTPL(session);
    }

    /**
     * rollback the row actions from start index in list and
     * the given timestamp
     */
    public void rollbackPartial(Session session, int start, long timestamp) {

        int limit = session.rowActionList.size();

        if (start == limit) {
            return;
        }

        for (int i = limit - 1; i >= start; i--) {
            RowAction action = (RowAction) session.rowActionList.get(i);

            if (action == null || action.type == RowActionBase.ACTION_NONE
                    || action.type == RowActionBase.ACTION_DELETE_FINAL) {
                continue;
            }

            Row row = action.memoryRow;

            if (row == null) {
                row = (Row) action.store.get(action.getPos(), false);
            }

            if (row == null) {
                continue;
            }

            writeLock.lock();

            try {
                action.rollback(session, timestamp);

                int type = action.mergeRollback(session, timestamp);

                if (action.type == RowActionBase.ACTION_DELETE_FINAL) {
                    if (action.deleteComplete) {
                        continue;
                    }

                    action.deleteComplete = true;
                }

                action.store.rollbackRow(session, row, type, txModel);
            } finally {
                writeLock.unlock();
            }
        }

        session.rowActionList.setSize(start);
    }

    public RowAction addDeleteAction(Session session, Table table,
                                     PersistentStore store, Row row,
                                     int[] changedColumns) {

        RowAction action = store.addDeleteActionToRow(session, row,
            changedColumns, true);

        if (table.tableType == TableBase.TEMP_TABLE) {
            store.delete(session, row);

            row.rowAction = null;

            if (table.persistenceScope == Table.SCOPE_ROUTINE) {
                return action;
            }
        }

        session.rowActionList.add(action);

        return action;
    }

    public void addInsertAction(Session session, Table table,
                                PersistentStore store, Row row,
                                int[] changedColumns) {

        RowAction action = row.rowAction;

        if (action == null) {
/*
            System.out.println("null insert action " + session + " "
                               + session.actionTimestamp);
*/
            throw Error.runtimeError(ErrorCode.GENERAL_ERROR,
                                     "null insert action ");
        }

        store.indexRow(session, row);

        if (table.persistenceScope == Table.SCOPE_ROUTINE) {
            row.rowAction = null;

            return;
        }

        session.rowActionList.add(action);
    }

// functional unit - accessibility of rows
    public boolean canRead(Session session, PersistentStore store, Row row,
                           int mode, int[] colMap) {

        RowAction action = row.rowAction;

        if (action == null) {
            return true;
        }

        if (action.table.tableType == TableBase.TEMP_TABLE) {
            return true;
        }

        return action.canRead(session, TransactionManager.ACTION_READ);
    }

    /**
     * add a list of actions to the end of queue
     */
    void addToCommittedQueue(Session session, Object[] list) {

        synchronized (committedTransactionTimestamps) {

            // add the txList according to commit timestamp
            committedTransactions.addLast(list);

            // get session commit timestamp
            committedTransactionTimestamps.addLast(session.actionTimestamp);
/* debug 190
            if (committedTransactions.size() > 64) {
                System.out.println("******* excessive transaction queue");
            }
// debug 190 */
        }
    }

    /**
     * expire all committed transactions that are no longer in scope
     */
    void mergeExpiredTransactions(Session session) {

        long timestamp = getFirstLiveTransactionTimestamp();

        while (true) {
            long     commitTimestamp = 0;
            Object[] actions         = null;

            synchronized (committedTransactionTimestamps) {
                if (committedTransactionTimestamps.isEmpty()) {
                    break;
                }

                commitTimestamp = committedTransactionTimestamps.getFirst();

                if (commitTimestamp < timestamp) {
                    committedTransactionTimestamps.removeFirst();

                    actions = (Object[]) committedTransactions.removeFirst();
                } else {
                    break;
                }
            }

            mergeTransaction(actions, 0, actions.length, commitTimestamp);
            finaliseRows(session, actions, 0, actions.length);
        }
    }

    public void beginTransaction(Session session) {

        writeLock.lock();

        try {
            if (!session.isTransaction) {
                beginTransactionCommon(session);
                liveTransactionTimestamps.addLast(
                    session.transactionTimestamp);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * add session to the end of queue when a transaction starts
     * (depending on isolation mode)
     */
    public void beginAction(Session session, Statement cs) {

        writeLock.lock();

        try {
            if (hasExpired) {
                session.redoAction = true;

                return;
            }

            cs = updateCurrentStatement(session, cs);

            if (cs == null) {
                return;
            }

            boolean canProceed = setWaitedSessionsTPL(session, cs);

            if (canProceed) {
                session.isPreTransaction = true;

                if (session.tempSet.isEmpty()) {
                    lockTablesTPL(session, cs);

                    // we don't set other sessions that would now be waiting for this one too
                } else {
                    setWaitingSessionTPL(session);
                }
            } else {
                session.abortTransaction = true;
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * add session to the end of queue when a transaction starts
     * (depending on isolation mode)
     */
    public void beginActionResume(Session session) {

        writeLock.lock();

        try {
            if (session.isTransaction) {
                session.actionTimestamp      = getNextGlobalChangeTimestamp();
                session.actionStartTimestamp = session.actionTimestamp;
            } else {
                beginTransactionCommon(session);
                liveTransactionTimestamps.addLast(
                    session.transactionTimestamp);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void resetSession(Session session, Session targetSession,
                             long statementTimestamp, int mode) {
        super.resetSession(session, targetSession, statementTimestamp, mode);
    }

    /**
     * remove session from queue when a transaction ends
     * and expire any committed transactions
     * that are no longer required. remove transactions ended before the first
     * timestamp in liveTransactionsSession queue
     */
    private void endTransaction(Session session) {

        long timestamp = session.transactionTimestamp;
        int  index     = liveTransactionTimestamps.indexOf(timestamp);

        if (index >= 0) {
            transactionCount.decrementAndGet();
            liveTransactionTimestamps.remove(index);
            mergeExpiredTransactions(session);
        }
    }
}
