package projekt.cloud.piece.server

import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_CREATE_PERIOD
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_END_TIME
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_STATUS
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_STATUS_ENABLE
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_TOKEN
import projekt.cloud.piece.server.DatabaseConstant.TABLE_AUTH_TOKEN_USERNAME
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_GROUP
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_GROUP_UNKNOWN
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_PASSWORD
import projekt.cloud.piece.server.DatabaseConstant.TABLE_USER_USERNAME
import projekt.cloud.piece.server.base.BaseSQLServlet
import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.Locale

@WebServlet(name = "authServlet", value = ["/auth"])
class AuthServlet: BaseSQLServlet() {
    
    private companion object {
        const val REQUEST_METHOD = "method"
        const val METHOD_TOKEN = "token"
        const val METHOD_LOGIN = "login"
        
        const val TIME_FORMAT = "yyyyMMddHHmmssSSS"
    
        const val ERROR_CODE_UNKNOWN_METHOD = 400
        const val ERROR_UNKNOWN_METHOD = "Method not specified / unknown"
    
        const val ERROR_CODE_NOT_ENOUGH_PARAMETER = 400
        const val ERROR_NOT_ENOUGH_PARAMETER = "Parameter not enough"
    
        const val PERIOD_FOREVER = "0"
        const val PERIOD_ONE_DAY = "1"
        const val PERIOD_SEVEN_DAYS = "2"
        const val PERIOD_THIRTY_DAYS = "3"
        
        /** LOGIN **/
        const val SUCCESS_CODE_LOGIN = 200
    
        const val LOGIN_USERNAME = TABLE_USER_USERNAME
        const val LOGIN_PASSWORD = TABLE_USER_PASSWORD
        const val LOGIN_PERIOD = TABLE_AUTH_TOKEN_CREATE_PERIOD
    
        const val LOGIN_RESPONSE_FAIL = "0"
        const val LOGIN_RESPONSE_SUCCESS = "1"
        const val LOGIN_RESPONSE_USER_GROUP = TABLE_USER_GROUP
        const val LOGIN_RESPONSE_USERNAME = TABLE_USER_USERNAME
        const val LOGIN_RESPONSE_TOKEN = METHOD_TOKEN
        const val LOGIN_RESPONSE_END_TIME = TABLE_AUTH_TOKEN_END_TIME
        
        /** TOKEN **/
        const val TOKEN_TOKEN = METHOD_TOKEN
        const val TOKEN_RESPONSE_FAIL = "0"
        const val TOKEN_RESPONSE_SUCCESS = "1"
        const val TOKEN_RESPONSE_USERNAME = TABLE_USER_USERNAME
        const val TOKEN_RESPONSE_USER_GROUP = TABLE_USER_GROUP
        const val TOKEN_RESPONSE_PERIOD = TABLE_AUTH_TOKEN_CREATE_PERIOD
        const val TOKEN_RESPONSE_END_TIME = TABLE_AUTH_TOKEN_END_TIME
    
    }
    
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        super.doGet(req, resp)
        when (val method = req.parameterMap[REQUEST_METHOD]?.first()) {
            METHOD_LOGIN -> req.methodLogin(resp)
            METHOD_TOKEN -> req.methodToken(resp)
            else -> resp.methodUnknown(method)
        }
    }
    
    private fun HttpServletRequest.methodLogin(resp: HttpServletResponse) {
        val username = parameterMap[LOGIN_USERNAME]?.first() ?: return resp.sendError(ERROR_CODE_NOT_ENOUGH_PARAMETER, ERROR_NOT_ENOUGH_PARAMETER)
        val password = parameterMap[LOGIN_PASSWORD]?.first() ?: return resp.sendError(ERROR_CODE_NOT_ENOUGH_PARAMETER, ERROR_NOT_ENOUGH_PARAMETER)
        val period = parameterMap[LOGIN_PERIOD]?.first() ?: return resp.sendError(ERROR_CODE_NOT_ENOUGH_PARAMETER, ERROR_NOT_ENOUGH_PARAMETER)
        
        val userGroup = sqlServer.createStatement()
            .executeQuery("SELECT * FROM $TABLE_USER WHERE $TABLE_USER_USERNAME='$username' AND $TABLE_USER_PASSWORD='$password' ;")
            .use {
                when {
                    !it.next() -> TABLE_USER_GROUP_UNKNOWN
                    else -> it.getString(TABLE_USER_GROUP)
                }
            }.toString()
        
        resp.status = SUCCESS_CODE_LOGIN
        resp.contentType = CONTENT_TYPE
        val createTime = System.currentTimeMillis()
        val endTime = getEndTime(period, createTime)
        val token = try {
            sqlServer.createStatement().createToken(username, createTime, endTime, period)
        } catch (e: Exception) {
            return resp.writer.write(createJson(METHOD_LOGIN, LOGIN_RESPONSE_FAIL))
        }
        
        resp.writer.write(createJson(METHOD_LOGIN, LOGIN_RESPONSE_SUCCESS) {
            addProperty(LOGIN_RESPONSE_USERNAME, username)
            addProperty(LOGIN_RESPONSE_TOKEN, token)
            addProperty(LOGIN_RESPONSE_USER_GROUP, userGroup)
            addProperty(LOGIN_RESPONSE_END_TIME, SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(endTime))
        })
    }
    
    private fun Statement.createToken(username: String, createTime: Long, endTime: Long, period: String): String {
        val token = getToken(username, createTime, period)
        execute(
            "INSERT INTO $TABLE_AUTH_TOKEN VALUES ( '$username' , '$token' , '$period' , '$createTime' , '$endTime' , '$TABLE_AUTH_TOKEN_STATUS_ENABLE' ) ;"
        )
        closeOnCompletion()
        return token
    }
    
    private fun getToken(username: String, time: Long, period: String) = "$username$period$time"
    
    private fun getEndTime(period: String, time: Long) = when (period) {
        PERIOD_FOREVER -> time
        PERIOD_ONE_DAY -> time + 86400000L
        PERIOD_SEVEN_DAYS -> time + 604800000L
        PERIOD_THIRTY_DAYS -> time +  2592000000L
        else -> time
    }
    
    private fun HttpServletRequest.methodToken(resp: HttpServletResponse) {
        val token = parameterMap[TOKEN_TOKEN]?.first() ?: return resp.sendError(ERROR_CODE_NOT_ENOUGH_PARAMETER, ERROR_NOT_ENOUGH_PARAMETER)
        val queryCursor = sqlServer.createStatement().executeQuery(
            "SELECT * FROM $TABLE_AUTH_TOKEN WHERE " +
                "$TABLE_AUTH_TOKEN_TOKEN='$token' AND " +
                // "$TABLE_AUTH_TOKEN_END_TIME<'${System.currentTimeMillis()}' AND " +
                "$TABLE_AUTH_TOKEN_STATUS='$TABLE_AUTH_TOKEN_STATUS_ENABLE' " +
                " ;"
        )
        resp.status = SUCCESS_CODE_LOGIN
        resp.contentType = CONTENT_TYPE
        
        if (!queryCursor.next()) {
            return resp.writer.write(createJson(METHOD_TOKEN, TOKEN_RESPONSE_FAIL))
        }
    
        when (val period = queryCursor.getString(TABLE_AUTH_TOKEN_CREATE_PERIOD)) {
            PERIOD_FOREVER -> {
                val username = queryCursor.getString(TABLE_AUTH_TOKEN_USERNAME)
                val endTime = queryCursor.use { it.getLong(TABLE_AUTH_TOKEN_END_TIME) }
                val userGroup = sqlServer.createStatement().executeQuery("SELECT * FROM $TABLE_USER WHERE $TABLE_USER_USERNAME='$username' ;").use {
                    if (!it.next()) {
                        return resp.writer.write(createJson(METHOD_TOKEN, TOKEN_RESPONSE_FAIL))
                    }
                    it.getString(TABLE_USER_GROUP)
                }
                resp.writer.write(createJson(METHOD_TOKEN, TOKEN_RESPONSE_SUCCESS) {
                    addProperty(TOKEN_RESPONSE_USERNAME, username)
                    addProperty(TOKEN_RESPONSE_USER_GROUP, userGroup)
                    addProperty(TOKEN_RESPONSE_PERIOD, period)
                    addProperty(TOKEN_RESPONSE_END_TIME, SimpleDateFormat(TIME_FORMAT).format(endTime))
                })
            }
            else -> resp.writer.write(createJson(METHOD_TOKEN, TOKEN_RESPONSE_FAIL))
        }
    }
    
    private fun HttpServletResponse.methodUnknown(method: String?) = sendError(ERROR_CODE_UNKNOWN_METHOD, "$ERROR_UNKNOWN_METHOD $method")

}