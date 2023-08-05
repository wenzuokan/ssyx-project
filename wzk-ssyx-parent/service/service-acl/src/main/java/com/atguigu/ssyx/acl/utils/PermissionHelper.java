package com.atguigu.ssyx.acl.utils;

import com.atguigu.ssyx.model.acl.Permission;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WenZK
 * @create 2023-06-14
 *
 */
public class PermissionHelper {

    public static List<Permission> buildPermission(List<Permission> allList){
        //创建最终数据封装集合list
        List<Permission> trees=new ArrayList<>();
        //遍历所有菜单list集合，得到第一层数据，pid=0
        for (Permission permission:allList){

            if (permission.getPid()==0){
                permission.setLevel(1);
                //调用方法，从第一层开始往下找
                trees.add(findChildren(permission,allList));
            }
        }
        return trees;
    }

    //递归向下去找
    private static Permission findChildren(Permission permission,List<Permission> allList){
        permission.setChildren(new ArrayList<Permission>());
        //遍历allList所有菜单数据
        //判断当前id=pid是否一样，封装，递归往下找
        for (Permission it:allList){
            if (it.getPid().longValue()==permission.getId().longValue()){
                int level = permission.getLevel() + 1;
                it.setLevel(level);
                if (permission.getChildren()==null){
                    permission.setChildren(new ArrayList<>());
                }
                //封装下一层数据
                permission.getChildren().add(findChildren(it,allList));
            }
        }
        return permission;
    }
}
