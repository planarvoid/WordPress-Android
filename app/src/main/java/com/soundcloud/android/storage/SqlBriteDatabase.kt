package com.soundcloud.android.storage

import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.utils.OpenForTesting
import com.squareup.sqlbrite2.BriteDatabase
import com.squareup.sqlbrite2.SqlBrite
import com.squareup.sqldelight.RowMapper
import com.squareup.sqldelight.SqlDelightCompiledStatement
import com.squareup.sqldelight.SqlDelightStatement
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import java.util.ArrayList
import javax.inject.Named

@OpenForTesting
class SqlBriteDatabase(databaseOpenHelper: SQLiteOpenHelper, @Named(ApplicationModule.RX_HIGH_PRIORITY) scheduler: Scheduler) {
    protected val briteDatabase: BriteDatabase = SqlBrite.Builder().build().wrapDatabaseHelper(databaseOpenHelper, scheduler)

    fun writableDatabase(): SQLiteDatabase {
        return briteDatabase.writableDatabase
    }

    @Throws(SQLException::class)
    fun insert(table: String, statement: SQLiteStatement): Long = briteDatabase.executeInsert(table, statement)

    @Throws(SQLException::class)
    fun insert(table: String, statement: SqlDelightCompiledStatement): Long = insert(table, statement.program)

    @Throws(SQLException::class)
    fun update(table: String, statement: SqlDelightCompiledStatement): Int = briteDatabase.executeUpdateDelete(table, statement.program)

    @Throws(SQLException::class)
    fun updateOrDelete(table: String, statement: SQLiteStatement): Long = briteDatabase.executeUpdateDelete(table, statement).toLong()

    @Throws(SQLException::class)
    fun updateOrDelete(table: String, statement: SqlDelightCompiledStatement): Long = updateOrDelete(table, statement.program)

    @Throws(SQLException::class)
    fun updateOrDeleteAsync(table: String, statement: SQLiteStatement): Single<Long> {
        return Single.fromCallable { updateOrDelete(table, statement) }
    }

    fun clear(table: String): Int = briteDatabase.delete(table, null)

    fun runInTransaction(runnable: Runnable): BriteDatabase.Transaction = runInTransaction { runnable.run() }

    fun runInTransaction(runnable: () -> Unit): BriteDatabase.Transaction {
        val transaction = briteDatabase.newTransaction()
        try {
            runnable.invoke()
            transaction.markSuccessful()
        } finally {
            transaction.end()
        }
        return transaction
    }

    fun runInTransactionAsync(runnable: () -> Unit): Single<BriteDatabase.Transaction> {
        return Single.fromCallable { runInTransaction(runnable) }
    }

    @Throws(SQLException::class)
    fun batchInsert(table: String, statements: List<SQLiteStatement>) {
        for (statement in statements) {
            briteDatabase.executeInsert(table, statement)
        }
    }

    fun execute(statement: SqlDelightStatement) {
        briteDatabase.executeAndTrigger(statement.tables, statement.statement, *statement.args)
    }

    fun <T> executeObservableQuery(mapper: RowMapper<T>, tableName: String, query: String, vararg args: String): Observable<List<T>> {
        val queryObservable = briteDatabase.createQuery(tableName, query, *args)
        return queryObservable.mapToList { mapper.map(it) }
    }

    fun <T> executeAsyncQuery(sqlDelightStatement: SqlDelightStatement, selectionItemMapper: RowMapper<T>): Single<List<T>> {
        return Single.fromCallable { executeQuery(sqlDelightStatement, selectionItemMapper) }
    }

    fun <T> executeAsyncSelectItemQuery(sqlDelightStatement: SqlDelightStatement, selectionItemMapper: RowMapper<T>): Maybe<T> {
        return executeAsyncQuery(sqlDelightStatement, selectionItemMapper).filter { list -> list.size == 1 }.map { list -> list[0] }
    }

    fun <T> executeQuery(sqlDelightStatement: SqlDelightStatement, itemMapper: RowMapper<T>): List<T> {
        val resultList = ArrayList<T>()
        val cursor = briteDatabase.query(sqlDelightStatement.statement, *sqlDelightStatement.args)
        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                resultList.add(itemMapper.map(cursor))
            }
        }
        return resultList
    }

    fun <T> executeSelectItemQuery(sqlDelightStatement: SqlDelightStatement, itemMapper: RowMapper<T>): T? {
        val results = executeQuery(sqlDelightStatement, itemMapper)
        return if (results.size == 1) results[0] else null
    }
}
