package com.hy.wx_auth.auth;

import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author hy
 */
@CrossOrigin
@RestController
@RequestMapping("wx")
public class WxAuthController {

    @Resource
    private WxAuthService wxAuthService;

    /**
     * 获取获取openId和会话密钥
     *
     * @param code
     * @return
     */
    @GetMapping("/getOpenIdAndSessionKey")
    public JSONObject getOpenIdAndSessionKey(@RequestParam("code") String code) {
        return wxAuthService.getOpenIdAndSessionKey(code);
    }

    /**
     * 获取用户手机号
     *
     * @param params
     * @return
     */
    @PostMapping("/getPhone")
    public JSONObject getPhone(@RequestBody Map<String, Object> params) {
        return wxAuthService.getPhone(params);
    }

}
