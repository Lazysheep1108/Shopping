package cn.wolfcode.web.controller;

import cn.wolfcode.common.constants.CommonConstants;
import cn.wolfcode.common.domain.UserInfo;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.UserLogin;
import cn.wolfcode.domain.UserResponse;
import cn.wolfcode.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/users")
public class UserController {

    private final  IUserService userService;

    public UserController (IUserService userService){
        this.userService = userService;
    }

    @GetMapping("/{phone}")
    public  Result<UserInfo> getByPhone(@PathVariable String phone){
        return Result.success(userService.getByPhone(phone));
    }


}
