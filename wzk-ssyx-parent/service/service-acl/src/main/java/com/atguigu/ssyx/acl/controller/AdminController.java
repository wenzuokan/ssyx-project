package com.atguigu.ssyx.acl.controller;

import com.atguigu.ssyx.acl.service.AdminService;
import com.atguigu.ssyx.acl.service.RoleService;
import com.atguigu.ssyx.common.result.Result;

import com.atguigu.ssyx.common.utils.MD5;
import com.atguigu.ssyx.model.acl.Admin;
import com.atguigu.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author WenZK
 * @create 2023-06-13
 *
 */
@Api(tags = "用户接口")
@RestController
@RequestMapping("/admin/acl/user")
//@CrossOrigin
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private RoleService roleService;

    //为用户分配角色
    @ApiOperation("为用户分配角色")
    @PostMapping("doAssign")
    public Result doAssign(@RequestParam Long adminId,
                           @RequestParam Long[] roleId){
        roleService.saveAdminRole(adminId,roleId);
        return Result.ok(null);
    }

    //获取用户角色，和根据用户id查询用户分配角色列表
    @ApiOperation("获取用户角色")
    @GetMapping("toAssign/{adminId}")
    public Result toAssign(@PathVariable Long adminId){
        //返回的map集合包含两部分数据：所有角色 和 为用户分配角色列表
       Map<String,Object> map= roleService.getRoleByAdminId(adminId);
       return Result.ok(map);
    }

    //1 用户列表
    @ApiOperation("用户列表")
    @GetMapping("{current}/{limit}")
    public Result list(@PathVariable Long current,
                       @PathVariable Long limit,
                       AdminQueryVo adminQueryVo){
        Page<Admin> pageParam=new Page<Admin>(current,limit);
        IPage<Admin> pageModel= adminService.selectPageUser(pageParam,adminQueryVo);
        return Result.ok(pageModel);
    }

    //2 id查询
    @ApiOperation("id查询")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id){
        Admin admin = adminService.getById(id);
        return Result.ok(admin);
    }

    //3 添加用户
    @ApiOperation("添加用户")
    @PostMapping("save")
    public Result save(@RequestBody Admin admin){
        //获取到输入的密码
        String password=admin.getPassword();
        //对输入密码进行加密 MD5
        String passwordMD5 = MD5.encrypt(password);
        //设置到admin对象里面
        admin.setPassword(passwordMD5);
        //调用方法添加
        adminService.save(admin);
        return Result.ok(null);
    }

    //4 修改删除
    @ApiOperation("修改")
    @PutMapping("update")
    public Result update(@RequestBody Admin admin){
        adminService.updateById(admin);
        return Result.ok(null);
    }

    //5 id删除
    @ApiOperation("id删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        adminService.removeById(id);
        return Result.ok(null);
    }

    //6 批量删除
    @ApiOperation("批量删除")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList){
        adminService.removeByIds(idList);
        return Result.ok(null);
    }

}
