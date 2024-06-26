package com.github.yulichang.test.util;

import com.baomidou.mybatisplus.annotation.DbType;

@SuppressWarnings("unused")
public class EnabledIf {

    public static boolean runWithMysql() {
        return DbTypeUtil.getDbType() == DbType.MYSQL;
    }

    public static boolean runWithPgsql() {
        return DbTypeUtil.getDbType() == DbType.POSTGRE_SQL;
    }

    public static boolean runWithExcludingOracle() {
        return DbTypeUtil.getDbType() != DbType.ORACLE;
    }
}
