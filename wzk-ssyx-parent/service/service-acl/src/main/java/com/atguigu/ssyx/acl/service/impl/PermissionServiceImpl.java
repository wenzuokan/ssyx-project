package com.atguigu.ssyx.acl.service.impl;

import com.atguigu.ssyx.acl.mapper.PermissionMapper;
import com.atguigu.ssyx.acl.service.PermissionService;
import com.atguigu.ssyx.acl.utils.PermissionHelper;
import com.atguigu.ssyx.model.acl.Permission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WenZK
 * @create 2023-06-14
 *
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    //查询所有菜单
    @Override
    public List<Permission> queryAllPermission() {
        //1 查询所有菜单
        List<Permission> allPermissionList = baseMapper.selectList(null);

        //2 转换要求数据格式
        List<Permission> result= PermissionHelper.buildPermission(allPermissionList);
        return result;
    }

    //递归删除菜单
    @Override
    public void removeChildById(Long id) {
        //1 创建idList，封装需要删除所有的菜单id
        List<Long> idList=new ArrayList<>();
        //根据当前菜单id获取当前菜单下面所有子菜单
        //如果子菜单下还有子菜单，都要获取到
        //递归找当前菜单子菜单
        this.getAllPermissionId(id,idList);

        baseMapper.deleteBatchIds(idList);
    }

    //递归找当前菜单子菜单
    private void getAllPermissionId(Long id,List<Long> idList){
        //根据当前菜单id查询子菜单
        LambdaQueryWrapper<Permission> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getPid,id);
        List<Permission> childList = baseMapper.selectList(wrapper);

        //递归查询是否还要子菜单
        childList.stream().forEach(item ->{
            //封装菜单id到idList集合里
            idList.add(item.getId());
            //递归
            this.getAllPermissionId(item.getId(),idList);
        });
    }
}
