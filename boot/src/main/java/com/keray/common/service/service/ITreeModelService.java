package com.keray.common.service.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keray.common.Wrappers;
import com.keray.common.entity.BSService;
import com.keray.common.entity.IBSEntity;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.service.model.base.BaseTreeModel;
import org.springframework.cache.annotation.Cacheable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author by keray
 * date:2019/8/16 16:17
 * 树形结构实体的操作服务
 */
public interface ITreeModelService<T extends BaseTreeModel<T, ID>, ID extends Serializable> extends BSService<T, ID> {

    /**
     * 设置所有下级
     *
     * @return
     */
    default T setChildren(T owner) {
        return setChildren(owner, -1, null);
    }

    /**
     * <p>
     * <h3>>作者 keray</h3>
     * <h3>>时间： 2019/8/16 16:08</h3>
     * 具有上下级的实体 设置树结构
     * 仅当n=-1时缓存
     * </p>
     *
     * @param n 获取多少级 -1 不限
     * @return <p>  </p>
     */
    default T setChildren(T owner, int n) {
        return setChildren(owner, n, Wrappers.query());
    }

    default T setChildrenNoCache(T owner) {
        return setChildren(owner, -1, null);
    }


    default T setChildrenNoCache(T owner, int n) {
        return setChildren(owner, n, Wrappers.query());
    }


    /**
     * <p>
     * <h3>>作者 keray</h3>
     * <h3>>时间： 2019/8/16 16:08</h3>
     * 具有上下级的实体 设置树结构
     * </p>
     *
     * @param parent
     * @param n      获取多少级 -1 不限
     * @return <p>  </p>
     */
    default T setChildren(T parent, int n, Wrapper<T> inputQueryWrapper) {
        if (n == 0 || parent == null) {
            return parent;
        }
        List<T> children;
        try {
            if (inputQueryWrapper == null) {
                inputQueryWrapper = Wrappers.lambdaQuery();
            }
            Wrapper<T> queryWrapper = null;
            if (inputQueryWrapper instanceof QueryWrapper<T> q) {
                queryWrapper = q.clone();
                ((QueryWrapper<T>) queryWrapper).eq("parent_id", parent.getId());
            }
            if (inputQueryWrapper instanceof LambdaQueryWrapper<T> q) {
                queryWrapper = q.clone();
                ((LambdaQueryWrapper<T>) queryWrapper).eq(T::getParentId, parent.getId());
            }
            if (queryWrapper == null) throw new BizRuntimeException();
            children = getMapper().selectList(queryWrapper);
            if (CollUtil.isEmpty(children)) {
                return parent;
            }
            children = children.stream().parallel().map(this::modelDetail).toList();
            parent.setChildren(children);
            for (T c : children) {
                // 深copy父节点 避免循环依赖
                T parentCopy = builder();
                BeanUtil.copyProperties(parent, parentCopy, "parent", "children");
                c.setParent(parentCopy);
                setChildren(c, n - 1, inputQueryWrapper);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parent;
    }

    // 获取所有下级id
    default List<ID> allChildrenIds(ID id, int n, Wrapper<T> inputQueryWrapper, List<ID> result) {
        if (n == 0) {
            return result;
        }
        if (inputQueryWrapper == null) {
            inputQueryWrapper = Wrappers.lambdaQuery();
        }
        Wrapper<T> queryWrapper = null;
        if (inputQueryWrapper instanceof QueryWrapper<T> q) {
            queryWrapper = q.clone();
            ((QueryWrapper<T>) queryWrapper).select("id").eq("parent_id", id);
        }
        if (inputQueryWrapper instanceof LambdaQueryWrapper<T> q) {
            queryWrapper = q.clone();
            ((LambdaQueryWrapper<T>) queryWrapper).select(T::getId).eq(T::getParentId, id);
        }
        if (queryWrapper == null) throw new BizRuntimeException();
        List<ID> childrenIds = getMapper().selectList(queryWrapper).stream().map(IBSEntity::getId).toList();
        if (CollUtil.isEmpty(childrenIds)) {
            return result;
        }
        result.addAll(childrenIds);
        for (ID i : childrenIds) {
            allChildrenIds(i, n - 1, inputQueryWrapper, result);
        }
        return result;
    }

    default List<ID> allChildrenIds(ID id) {
        return allChildrenIds(id, -1, null, new LinkedList<>());
    }


    default List<T> allChildren(ID id, int n, Wrapper<T> inputQueryWrapper, List<T> result) {
        if (n == 0) {
            return result;
        }
        if (inputQueryWrapper == null) {
            inputQueryWrapper = Wrappers.lambdaQuery();
        }
        Wrapper<T> queryWrapper = null;
        if (inputQueryWrapper instanceof QueryWrapper<T> q) {
            queryWrapper = q.clone();
            ((QueryWrapper<T>) queryWrapper).eq("parent_id", id);
        }
        if (inputQueryWrapper instanceof LambdaQueryWrapper<T> q) {
            queryWrapper = q.clone();
            ((LambdaQueryWrapper<T>) queryWrapper).eq(T::getParentId, id);
        }
        if (queryWrapper == null) throw new BizRuntimeException();
        List<T> childrenIds = getMapper().selectList(queryWrapper).stream().map(this::modelDetail).toList();
        if (CollUtil.isEmpty(childrenIds)) {
            return result;
        }
        result.addAll(childrenIds);
        for (T child : childrenIds) {
            allChildren(child.getId(), n - 1, inputQueryWrapper, result);
        }
        return result;
    }

    default List<T> allChildren(ID id) {
        return allChildren(id, -1, null, new LinkedList<>());
    }

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/10/25 17:24</h3>
     * 属性结构下一级节点
     * </p>
     *
     * @return <p> {@link List<String>} </p>
     * @throws
     */
    default List<ID> selectNextChildrenIds(ID id) {
        return allChildrenIds(id, 1, null, new LinkedList<>());
    }


    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/10/25 17:24</h3>
     * 属性结构下一级节点的id
     * </p>
     *
     * @return <p> {@link List<T>} </p>
     * @throws
     */
    default List<T> selectNextChildren(ID id) {
        return allChildren(id, 1, null, new LinkedList<>());
    }

    // 根据自身id获取父级
    default T parent(ID id, Wrapper<T> wrapper) {
        var m = selectOne(Wrappers.<T>lambdaQuery().select(T::getParentId).eq(T::getId, id));
        return parent(m, wrapper);
    }

    default T parent(ID id) {
        return parent(id, null);
    }


    // 根据自身获取父级
    default T parent(T owner) {
        return parent(owner, null);
    }

    default T parent(T owner, Wrapper<T> inputQueryWrapper) {
        if (owner == null) return null;
        ID parentId = owner.getParentId();
        if (parentIdIsNull(parentId)) return null;
        if (inputQueryWrapper == null) {
            inputQueryWrapper = Wrappers.lambdaQuery();
        }
        if (inputQueryWrapper instanceof LambdaQueryWrapper<T> q) {
            q.eq(T::getId, parentId);
        } else if (inputQueryWrapper instanceof QueryWrapper<T> q) {
            q.eq("id", parentId);
        } else {
            throw new BizRuntimeException();
        }
        return modelDetail(selectOne(inputQueryWrapper));
    }

    // 根据自身查询所有父节点
    default List<T> allParent(T owner, List<T> result, Wrapper<T> inputQueryWrapper, int n) {
        if (result == null) {
            result = new LinkedList<>();
        }
        if (n == 0 || owner == null) return result;
        var parentId = owner.getParentId();
        if (parentIdIsNull(parentId)) {
            result.add(owner);
            return result;
        }
        T parent = parent(owner.getParentId(), inputQueryWrapper);
        if (parent == null) return result;
        result.add(parent);
        return allParent(parent, result, inputQueryWrapper, n - 1);
    }


    default List<T> allParent(T owner, List<T> result, Wrapper<T> wrapper) {
        return allParent(owner, result, wrapper, -1);
    }

    default List<T> allParent(T owner, Wrapper<T> wrapper) {
        return allParent(owner, new LinkedList<>(), wrapper, -1);
    }

    default List<T> allParent(T owner) {
        return allParent(owner, new LinkedList<>(), null, -1);
    }

    // 根据自身id获取所有父级
    default List<T> allParent(ID id, List<T> result, Wrapper<T> inputQueryWrapper) {
        var m = selectOne(Wrappers.<T>lambdaQuery().select(T::getParentId).eq(T::getId, id));
        if (m == null) return result;
        return allParent(m, result, inputQueryWrapper, -1);
    }

    default List<T> allParent(ID id, Wrapper<T> wrapper) {
        return allParent(id, new LinkedList<>(), wrapper);
    }

    default List<T> allParent(ID id) {
        return allParent(id, null);
    }

    // 根据父级id获取所有父级
    default List<ID> allParentIdsByParentId(ID parentId, List<ID> result, Wrapper<T> inputQueryWrapper, int n) {
        if (result == null) {
            result = new LinkedList<>();
        }
        if (n == 0) {
            return result;
        }
        if (parentIdIsNull(parentId)) {
            return result;
        }
        if (inputQueryWrapper == null) {
            inputQueryWrapper = Wrappers.lambdaQuery();
        }
        Wrapper<T> queryWrapper;
        if (inputQueryWrapper instanceof LambdaQueryWrapper<T> q) {
            queryWrapper = q.clone();
            ((LambdaQueryWrapper<T>) queryWrapper).select(T::getParentId, T::getId).eq(T::getId, parentId);
        } else if (inputQueryWrapper instanceof QueryWrapper<T> q) {
            queryWrapper = q.clone();
            ((QueryWrapper<T>) queryWrapper).select("parent_id", "id").eq("id", parentId);
        } else {
            throw new BizRuntimeException();
        }
        T parent = selectOne(queryWrapper);
        if (parent == null) return result;
        result.add(parent.getId());
        return allParentIdsByParentId(parent.getParentId(), result, inputQueryWrapper, n - 1);
    }

    default List<ID> allParentIdsByParentId(ID parentId, Wrapper<T> wrapper) {
        return allParentIdsByParentId(parentId, new LinkedList<>(), wrapper, -1);
    }

    default List<ID> allParentIdsByParentId(ID parentId) {
        return allParentIdsByParentId(parentId, null, null, -1);
    }


    default List<ID> allParentIds(ID id, List<ID> result, Wrapper<T> wrapper) {
        var m = selectOne(Wrappers.<T>lambdaQuery().select(T::getParentId).eq(T::getId, id));
        if (m == null) return result;
        return allParentIdsByParentId(m.getParentId(), result, wrapper, -1);
    }

    default List<ID> allParentIds(ID id, Wrapper<T> wrapper) {
        return allParentIds(id, new LinkedList<>(), wrapper);
    }

    default List<ID> allParentIds(ID id) {
        return allParentIds(id, null);
    }

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/10/25 17:24</h3>
     * 节点详情获取
     * </p>
     *
     * @return <p> {@link T} </p>
     * @throws
     */
    default T modelDetail(T simpleModel) {
        return simpleModel;
    }

    default void treeProcess(T parent, TreeProcess<T> process) {
        List<T> children = parent.getChildren();
        if (CollUtil.isNotEmpty(children)) {
            for (T child : children) {
                process.apply(child, parent);
                treeProcess(child, process);
            }
        }
    }

    default void treeChildrenProcess(T parent, TreeChildrenProcess<T> process) {
        List<T> children = parent.getChildren();
        if (CollUtil.isNotEmpty(children)) {
            process.apply(children, parent);
            for (T child : children) {
                treeChildrenProcess(child, process);
            }
        }
    }

    default boolean parentIdIsNull(ID parentId) {
        if (parentId instanceof String) {
            return ObjectUtil.isEmpty(parentId) || "0".equals(parentId);
        }
        if (parentId instanceof Number num) {
            return num.longValue() == 0;
        }
        return false;
    }

    T builder();
}
