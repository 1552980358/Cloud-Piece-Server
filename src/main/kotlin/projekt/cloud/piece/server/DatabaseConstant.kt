package projekt.cloud.piece.server

object DatabaseConstant {
    
    const val TABLE_USER = "user"
    const val TABLE_USER_USERNAME = "username"
    const val TABLE_USER_PASSWORD = "password"
    const val TABLE_USER_GROUP = "user_group"
    const val TABLE_USER_GROUP_UNKNOWN = 0
    const val TABLE_USER_GROUP_ADMIN = 1
    const val TABLE_USER_GROUP_USER = 2
    
    const val DATABASE_NAME = "cloud_piece"
    
    const val CONFIG_FILE = "database.conf"
    
    const val TABLE_AUTH_TOKEN = "auth_token"
    const val TABLE_AUTH_TOKEN_USERNAME = "username"
    const val TABLE_AUTH_TOKEN_TOKEN = "token"
    const val TABLE_AUTH_TOKEN_CREATE_PERIOD = "period"
    const val TABLE_AUTH_TOKEN_CREATE_TIME = "create_time"
    const val TABLE_AUTH_TOKEN_END_TIME = "end_time"
    const val TABLE_AUTH_TOKEN_STATUS = "status"
    const val TABLE_AUTH_TOKEN_STATUS_ENABLE = "1"
    const val TABLE_AUTH_TOKEN_STATUS_DISABLE = "0"
    
}