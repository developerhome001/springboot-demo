package com.keray.common.mysql.handler;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.StringTypeHandler;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;

import java.security.SecureRandom;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 数据库加密字段处理器
 */

public class DataEncryptionHandler extends StringTypeHandler {


    public static final String ENCRYPTION_PREFIX = "04bba87803d5e127";

    // sm2加密后会是这个统一的前缀 SecureRandom改变会导致前缀变化，    公钥，私钥改动会 不会 导致前缀变化 在hex模式下130位
    public static final String SM2_PREFIX = ENCRYPTION_PREFIX + "88b07a912ae5368e7b2720bd19b93920bdbf2e9b6286da9c4e2bc093830dc0762b8e144b80ae18447b1bf6c670b93bda9a80795768af5a90";

    // 不能改 改了会导致前缀不一致
    public static final SecureRandom SECURE_RANDOM = new SecureRandom() {
        @Override
        public void nextBytes(byte[] bytes) {
            Arrays.fill(bytes, (byte) 1);
        }
    };

    private static final int SM2_SUFFIX_LEN = 64;

    private static final SM2 sm2;

    private static final ECPrivateKeyParameters ecPrivateKeyParameters;
    private static final ECPublicKeyParameters ecPublicKeyParameters;


    private static final String publicKey = "046576e3722a163b53b5b2c3e8211514097c5dda8d1bb4f6ded55cbb08755f78b833d449f388af22785f55b431562de49e08e248cbf47794cfed45cc8c7ef5799a";
    private static final String privateKey = "370292340450e0b45fdc8d4a7a3e11802ba883203b9c2fd3ce5e8a9f1e8426e7";


    static {
        ecPrivateKeyParameters = ECKeyUtil.decodePrivateKeyParams(SecureUtil.decode(privateKey));
        ecPublicKeyParameters = ECKeyUtil.decodePublicKeyParams(SecureUtil.decode(publicKey));
        sm2 = new SM2(ecPrivateKeyParameters, ecPublicKeyParameters);
        sm2.setMode(SM2Engine.Mode.C1C2C3);
    }


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    /**
     * 字段加密
     *
     * @param val
     * @return
     */
    public static String encrypt(String val) {
        if (val == null || val.isEmpty()) return null;
        if (val.startsWith(ENCRYPTION_PREFIX)) return val;
        // 完整的sm2加密字符串 不适用base64，因为base64无法产生统一长度后缀
        var completeEncrypt = HexUtil.encodeHexStr(sm2.encrypt(StrUtil.utf8Bytes(val), new ParametersWithRandom(ecPublicKeyParameters, SECURE_RANDOM)));
        // 删除公共前缀，这样存储数据可以小很多 （时间换空间）
        var encryptStr = completeEncrypt.substring(SM2_PREFIX.length());
        // 加上可识别的前缀
        return ENCRYPTION_PREFIX + encryptStr;
    }

    public static String encryptLeftPrefix(String val) {
        if (val == null) return null;
        var encryptStr = encrypt(val);
        // 值%时，删除后边64位的crc（类似）校验码
        return encryptStr.substring(0, encryptStr.length() - SM2_SUFFIX_LEN);
    }


    /**
     * 字段解密
     *
     * @param val
     * @return
     */
    public static String decrypt(String val) {
        if (val == null || !val.startsWith(ENCRYPTION_PREFIX)) return val;
        // 删除识别前缀
        val = val.substring(ENCRYPTION_PREFIX.length());
        // 恢复完整的加密串
        val = SM2_PREFIX + val;
        return new String(sm2.decrypt(val, KeyType.PrivateKey));
    }
}
