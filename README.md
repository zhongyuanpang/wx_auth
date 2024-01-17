# 获取微信openId、会话key、手机号



## 第一步：配置微信appid

[登录微信开放平台](https://mp.weixin.qq.com/)

找到开发管理

![image-20240117180322346](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401171803240.png)

![image-20240117180407865](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401171804624.png)

![image-20240117180449513](https://gitee.com/nice3527/drawingbed/raw/master/blog/202401171804104.png)





## 前端调用



```vue
<button class="cu-btn block lg bg-blue shadow margin" open-type="getPhoneNumber"
					@getphonenumber="getPhoneNumber">确定</button>
```



wxAuthUrl 就是后端的服务地址

```js

			/**
			 * @param {Object} e
			 * 获取手机号
			 */
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
											
											this.$emit("close")
											
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

