package com.hy.wx_auth.auth;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.Cipher;
import java.security.*;


@Service
public class WxAuthService {

    private static String APP_ID;
    private static String APP_SECRET;

    @Value("${wx.APP_ID}")
    public void setAppId(String appId) {
        APP_ID = appId;
    }

    @Value("${wx.APP_SECRET}")
    public void setAppSecret(String appSecret) {
        APP_SECRET = appSecret;
    }

    /**
     * 获取openId和会话密钥
     *
     * @param code
     * @return
     */
    public JSONObject getOpenIdAndSessionKey(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + APP_ID + "&secret=" + APP_SECRET + "&js_code=" + code + "&grant_type=authorization_code";
        JSONObject jsonObj = null;
        try {
            URL urlGet = new URL(url);
            HttpURLConnection http = (HttpURLConnection) urlGet.openConnection();
            http.setRequestMethod("GET"); // 必须是get方式请求
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            http.setDoOutput(true);
            http.setDoInput(true);
            System.setProperty("sun.net.client.defaultConnectTimeout", "30000");// 连接超时30秒
            System.setProperty("sun.net.client.defaultReadTimeout", "30000"); // 读取超时30秒
            http.connect();
            InputStream is = http.getInputStream();
            int size = is.available();
            byte[] jsonBytes = new byte[size];
            is.read(jsonBytes);
            String message = new String(jsonBytes, StandardCharsets.UTF_8);

            jsonObj = JSONUtil.parseObj(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonObj;
    }

    /**
     * 通过appId和appSecret 获取Token
     *
     * @return
     */
    public String getAccessToken() {

        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + APP_ID + "&secret=" + APP_SECRET;
        String accessToken = null;
        try {
            URL urlGet = new URL(url);
            HttpURLConnection http = (HttpURLConnection) urlGet.openConnection();
            http.setRequestMethod("GET"); // 必须是get方式请求
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            http.setDoOutput(true);
            http.setDoInput(true);
            System.setProperty("sun.net.client.defaultConnectTimeout", "30000");// 连接超时30秒
            System.setProperty("sun.net.client.defaultReadTimeout", "30000"); // 读取超时30秒
            http.connect();
            InputStream is = http.getInputStream();
            int size = is.available();
            byte[] jsonBytes = new byte[size];
            is.read(jsonBytes);
            String message = new String(jsonBytes, StandardCharsets.UTF_8);

            // 解析 JSON 字符串
            JSONObject jsonObj = JSONUtil.parseObj(message);
            accessToken = jsonObj.getStr("access_token");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accessToken;
    }

    /**
     * 获取用户手机号
     *
     * @param params
     * @return
     */
    public JSONObject getPhone(Map<String, Object> params) {

        String sessionkey = params.get("session_key").toString();
        String encryptedData = params.get("encryptedData").toString();
        String iv = params.get("iv").toString();

        return wXBizDataCrypt(encryptedData, sessionkey, iv);
    }

    /**
     * 微信小程序加密数据解密：用于getUserInfo、getPhoneNumber
     *
     * @param encryptedData
     * @param sessionKey
     * @param iv
     * @return
     * @throws Exception
     */
    private JSONObject wXBizDataCrypt(String encryptedData, String sessionKey, String iv) {

        // 被加密的数据
        byte[] dataByte = Base64.decode(encryptedData);
        // 加密秘钥
        byte[] keyByte = Base64.decode(sessionKey);
        // 偏移量
        byte[] ivByte = Base64.decode(iv);
        try {
            // 如果密钥不足16位，那么就补足.这个if中的内容很重要
            int base = 16;
            if (keyByte.length % base != 0) {
                int groups = keyByte.length / base + 1;
                byte[] temp = new byte[groups * base];
                Arrays.fill(temp, (byte) 0);
                System.arraycopy(keyByte, 0, temp, 0, keyByte.length);
                keyByte = temp;
            }
            // 初始化
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecretKeySpec spec = new SecretKeySpec(keyByte, "AES");
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
            parameters.init(new IvParameterSpec(ivByte));
            cipher.init(Cipher.DECRYPT_MODE, spec, parameters);// 初始化
            byte[] resultByte = cipher.doFinal(dataByte);
            if (null != resultByte && resultByte.length > 0) {
                String message = new String(resultByte, StandardCharsets.UTF_8);

                // 解析 JSON 字符串
                return JSONUtil.parseObj(message);
            }
        } catch (Exception e) {
            System.out.println("解密加密信息报错" + e.getMessage());
        }
        return null;
    }
}
