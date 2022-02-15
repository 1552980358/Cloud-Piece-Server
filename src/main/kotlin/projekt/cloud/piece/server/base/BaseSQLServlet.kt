package projekt.cloud.piece.server.base

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import projekt.cloud.piece.server.DatabaseConstant
import projekt.cloud.piece.server.isSQLServerInitialized
import projekt.cloud.piece.server.setSQLServer
import java.io.File
import java.sql.DriverManager

open class BaseSQLServlet: HttpServlet() {
    
    private companion object {
        const val ERROR_CODE_SQL_NOT_INITIALIZED = 405
        const val ERROR_SQL_NOT_INITIALIZED = "SQL Database is not initialized"
    }
    
    private val warPath get() = servletContext.getRealPath("/")
    protected val configureFile by lazy { File(warPath, DatabaseConstant.CONFIG_FILE) }
    
    override fun init() {
        if (!isSQLServerInitialized && configureFile.exists()) {
            try {
                val file = configureFile.readLines()
                val connection = DriverManager.getConnection(
                    file[0].run { substring(indexOf('=') + 1) },
                    file[1].run { substring(indexOf('=') + 1) },
                    file[2].run { substring(indexOf('=') + 1) }
                )
                connection.createStatement().execute("USE ${DatabaseConstant.DATABASE_NAME}")
                setSQLServer(connection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        if (!isSQLServerInitialized) {
            return resp.sendError(ERROR_CODE_SQL_NOT_INITIALIZED, ERROR_SQL_NOT_INITIALIZED)
        }
    }
    
    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        if (!isSQLServerInitialized) {
            return resp.sendError(ERROR_CODE_SQL_NOT_INITIALIZED, ERROR_SQL_NOT_INITIALIZED)
        }
    }
    
}