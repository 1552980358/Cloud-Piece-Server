package projekt.cloud.piece.server

import java.sql.Connection

private var sqlServerConnection: Connection? = null

@Synchronized
fun setSQLServer(connection: Connection?) {
    sqlServerConnection = connection
}

@Synchronized
fun removeSQLServer() = setSQLServer(null)

val isSQLServerInitialized get() = sqlServerConnection == null

val sqlServer get() = sqlServerConnection!!