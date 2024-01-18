# 小程序获取微信信息、手机号



## 获取微信头像、用户名

```vue
// 获取头像
<button class="avatar-wrapper" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
    <image class="u-avatar" :src="avatarUrl || '../../static/head.png'"></image>
</button>

// 获取微信昵称
<input type="nickname" v-module="nickName" placeholder="请输入昵称" @blur="onBlur" />
```

```js
			/**
			 * @param {Object} e
			 * 获取头像
			 */
			onChooseAvatar(e) {
				this.avatarUrl = e.detail.avatarUrl
			},

			/**
			 * @param {Object} e
			 * 获取用户名
			 */
			onBlur(e) {
				this.nickName = e.detail.value
			},
```



![image-20240118092232164](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401180922302.png)

![image-20240118092311106](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401180923880.png)



## 获取手机号

```vue
<button open-type="getPhoneNumber" @getphonenumber="getPhoneNumber">获取手机号</button>
```

![image-20240118094358954](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401180944794.png)



`wxAuthUrl` 对应的后端地址

注意！！ 获取手机号必须要企业认证过的`appId`,个人开发者不可用

- `uni.login` 获取code吗
- `${wxAuthUrl}/wx/getOpenIdAndSessionKey` 根据code从获取获取微信`openId`和`session_key`
- `${wxAuthUrl}/wx/getPhone` 根据`session_key`及加密文去后端解密获取手机号

```js
getPhoneNumber(e) {
    if (e.detail.errMsg == "getPhoneNumber:ok") { // 用户允许或去手机号
        uni.login({
            provider: 'weixin',
            success: (res) => {
                // 获取openId和密钥
                uni.request({
                    url: `${wxAuthUrl}/wx/getOpenIdAndSessionKey`,
                    method: 'GET',
                    data: {
                        code: res.code,
                    },
                    success: (cts) => {
                        const {openid,session_key} = cts.data

                        uni.request({
                            url: `${wxAuthUrl}/wx/getPhone`,
                            method: "POST",
                            data: {
                                iv: e.detail.iv,
                                encryptedData: e.detail.encryptedData,
                                session_key: session_key
                            },
                            success: (respone) => {
                                const {phoneNumber} = respone.data

                                this.SET_USER_INFO({
                                    avatar:this.avatarUrl,
                                    nickname:this.nickName,
                                    openId:openid,
                                    phone:phoneNumber
                                })
                                
                                uni.showToast({
                                    icon:'success',
                                    title:'登录成功!'
                                })

                                setTimeout(()=>{
                                    uni.switchTab({
                                        url: "/pages/home/home"
                                    })
                                },500)

                            }
                        })
                    }
                });
            }
        });
    }
},
```





## 后端代码

[完整代码地址](https://github.com/zhongyuanpang/wx_auth)



### 配置微信appid

[登录微信开放平台](https://mp.weixin.qq.com/)

找到开发管理

![image-20240117180322346](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401171803240.png)

![image-20240117180407865](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401171804624.png)

![image-20240117180449513](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401171804104.png)



### 获取openId和会话密钥

```java
@GetMapping("/getOpenIdAndSessionKey")
public JSONObject getOpenIdAndSessionKey(@RequestParam("code") String code) {
    return wxAuthService.getOpenIdAndSessionKey(code);
}
```

```java
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
```



### 获取手机号

```java
@PostMapping("/getPhone")
public JSONObject getPhone(@RequestBody Map<String, Object> params) {
    return wxAuthService.getPhone(params);
}
```

```java
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
```

### 使用的依赖

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.6.5</version>
</dependency>

<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.64</version>
</dependency>
```

