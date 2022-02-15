package projekt.cloud.piece.server

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import projekt.cloud.piece.server.DatabaseConstant.DATABASE_NAME
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_CREATE_PERIOD
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_CREATE_TIME
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_END_TIME
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_STATUS
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_TOKEN
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_USERNAME
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_GROUP
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_GROUP_ADMIN
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_PASSWORD
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_USERNAME
import projekt.cloud.piece.server.base.BaseSQLServlet
import java.sql.DriverManager
import java.sql.Statement

@WebServlet(name = "initServlet", value = ["/init"])
class InitServlet: BaseSQLServlet() {
    
    private companion object {
        
        const val REQUEST_METHOD = "method"
        const val METHOD_CHECK = "check"
        const val METHOD_INITIAL = "initial"
        
        const val SQL_URL = "url"
        const val SQL_USERNAME = "username"
        const val SQL_PASSWORD = "password"
    
        const val ERROR_CODE_UNKNOWN_METHOD = 400
        const val ERROR_UNKNOWN_METHOD = "Method not specified / unknown"
        
        const val ERROR_CODE_NOT_ENOUGH_PARAMETER = 400
        const val ERROR_NOT_ENOUGH_PARAMETER = "Parameter not enough"
        
        const val ERROR_CODE_SQL_EXCEPTION = 500
        const val ERROR_SQL_EXCEPTION = "SQL Exception thrown"
        
        const val ERROR_CODE_SAVE_CONFIGURATION = 500
        const val ERROR_SAVE_CONFIGURATION = "Configuration saving exception thrown"
    
        const val SUCCESS_CODE_CHECK = 200
        const val SUCCESS_CHECK_FALSE = "0"
        const val SUCCESS_CHECK_TRUE = "1"
        
        const val SUCCESS_CODE_INITIAL = 200
        const val SUCCESS_INITIAL_TRUE = "1"
    }
    
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
       when (val method = req.parameterMap[REQUEST_METHOD]?.first()) {
           METHOD_CHECK -> resp.methodCheck()
           METHOD_INITIAL -> req.methodInitial(resp)
           else -> resp.methodUnknown(method)
       }
    }
    
    private fun HttpServletResponse.methodUnknown(method: String?) = sendError(ERROR_CODE_UNKNOWN_METHOD, "$ERROR_UNKNOWN_METHOD $method")
    
    private fun HttpServletResponse.methodCheck() {
        status = SUCCESS_CODE_CHECK
        contentType = CONTENT_TYPE
        writer.write(createJson(METHOD_CHECK, if (isSQLServerInitialized) SUCCESS_CHECK_TRUE else SUCCESS_CHECK_FALSE))
    }
    
    private fun HttpServletRequest.methodInitial(resp: HttpServletResponse) {
        val url = parameterMap[SQL_URL]?.first() ?: return resp.sendError(ERROR_CODE_NOT_ENOUGH_PARAMETER, ERROR_NOT_ENOUGH_PARAMETER)
        val username = parameterMap[SQL_USERNAME]?.first() ?: return resp.sendError(ERROR_CODE_NOT_ENOUGH_PARAMETER, ERROR_NOT_ENOUGH_PARAMETER)
        val password = parameterMap[SQL_PASSWORD]?.first() ?: return resp.sendError(ERROR_CODE_NOT_ENOUGH_PARAMETER, ERROR_NOT_ENOUGH_PARAMETER)
        
        println(url)
        println(username)
        println(password)
        
        val connection = try {
            DriverManager.getConnection(url, username, password)
        } catch (e: Exception) {
            return resp.sendError(ERROR_CODE_SQL_EXCEPTION, "0 $ERROR_SQL_EXCEPTION: $e")
        }
        
        try {
            connection.createStatement().createDatabase()
        } catch (e: Exception) {
            return resp.sendError(ERROR_CODE_SQL_EXCEPTION, "1 $ERROR_SQL_EXCEPTION: $e")
        }
        
        try {
            connection.createStatement().execute("USE $DATABASE_NAME")
        } catch (e: Exception) {
            return resp.sendError(ERROR_CODE_SQL_EXCEPTION, "2 $ERROR_SQL_EXCEPTION: $e")
        }
        
        try {
            connection.createStatement().createTables()
        } catch (e: Exception) {
            return resp.sendError(ERROR_CODE_SQL_EXCEPTION, "3 $ERROR_SQL_EXCEPTION: $e")
        }
        
        try {
            connection.createStatement().createAdmin(username, password)
        } catch (e: Exception) {
            return resp.sendError(ERROR_CODE_SQL_EXCEPTION, "5 $ERROR_SQL_EXCEPTION: $e ${url + DATABASE_NAME}")
        }
        
        try {
            configureFile.bufferedWriter().apply {
                write("$SQL_URL=$url")
                newLine()
                write("$SQL_USERNAME=$username")
                newLine()
                write("$SQL_PASSWORD=$password")
            }
        } catch (e: Exception) {
            return resp.sendError(ERROR_CODE_SAVE_CONFIGURATION, "7 $ERROR_SAVE_CONFIGURATION: $e")
        }
        
        setSQLServer(connection)
        
        resp.status = SUCCESS_CODE_INITIAL
        resp.contentType = CONTENT_TYPE
        resp.writer.write(createJson(METHOD_INITIAL, SUCCESS_INITIAL_TRUE))
    }
    
    private fun Statement.createDatabase() {
        execute("CREATE DATABASE IF NOT EXISTS $DATABASE_NAME ;")
        closeOnCompletion()
    }
    
    private fun Statement.createTables() =
        createUserTable().createTokenTable()
            .closeOnCompletion()
    
    private fun Statement.createUserTable() = apply {
        execute("CREATE TABLE IF NOT EXISTS $TABLE_USER( " +
            "$TABLE_USER_USERNAME VARCHAR(16) NOT NULL , " +
            "$TABLE_USER_PASSWORD VARCHAR(16) NOT NULL , " +
            "$TABLE_USER_GROUP VARCHAR(1) NOT NULL , " +
            "PRIMARY KEY ($TABLE_USER_USERNAME) " +
            ") ;")
    }
    
    private fun Statement.createTokenTable() = apply {
        execute(
            "CREATE TABLE IF NOT EXISTS $TABLE_AUTH_TOKEN ( " +
                "$TABLE_AUTH_TOKEN_USERNAME VARCHAR(16) NOT NULL , " +
                "$TABLE_AUTH_TOKEN_TOKEN VARCHAR(128) NOT NULL , " +
                "$TABLE_AUTH_TOKEN_CREATE_PERIOD VARCHAR(1) NOT NULL , " +
                "$TABLE_AUTH_TOKEN_CREATE_TIME LONG NOT NULL , " +
                "$TABLE_AUTH_TOKEN_END_TIME LONG NOT NULL, " +
                "$TABLE_AUTH_TOKEN_STATUS VARCHAR(1) NOT NULL , " +
                "PRIMARY KEY ( $TABLE_AUTH_TOKEN_TOKEN ) " +
                ") ;"
        )
    }
    
    private fun Statement.createAdmin(username: String, password: String) {
        execute("INSERT INTO $TABLE_USER VALUES ( '$username' , '$password' , '$TABLE_USER_GROUP_ADMIN' ) ;")
        closeOnCompletion()
    }
    
    
}