package com.keray.common.support;

import org.apache.ibatis.type.StringTypeHandler;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据库加密读取字段处理器
 */

public class StringEncryptionHandler extends StringTypeHandler {

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        var result = super.getNullableResult(rs, columnName);
        return DataEncryptionHandler.decrypt(result);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        var result = super.getNullableResult(rs, columnIndex);
        return DataEncryptionHandler.decrypt(result);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        var result = super.getNullableResult(cs, columnIndex);
        return DataEncryptionHandler.decrypt(result);

    }


}
