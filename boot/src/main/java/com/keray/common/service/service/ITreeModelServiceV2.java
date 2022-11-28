package com.keray.common.service.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keray.common.Wrappers;
import com.keray.common.entity.BSService;
import com.keray.common.entity.IBSEntity;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.service.model.base.BaseTreeModelV2;
import com.keray.common.utils.CommonUtil;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author by keray
 * date:2019/8/16 16:17
 * 树形结构实体的操作服务
 */
public interface ITreeModelServiceV2<T extends BaseTreeModelV2<T, ID>, ID extends Serializable> extends BSService<T, ID> {

    @Transactional(rollbackFor = Exception.class)
    @Override
    default T insert(T entity) {
        if (ObjectUtil.isNotEmpty(entity.getParentId()) && !entity.getParentId().equals("0")) {
            var parent = getById(entity.getParentId());
            entity.setPath(parent.getOPath());
            if (parent.getLeaf()) {
                parent.setLeaf(false);
                BSService.super.update(parent);
            }
        }
        return BSService.super.insert(updatePathProcess(entity, false));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    default T update(T entity) {
        return BSService.super.update(updatePathProcess(entity, true));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    default T simpleUpdate(T entity) {
        return BSService.super.simpleUpdate(updatePathProcess(entity, true));
    }

    default T updatePathProcess(T entity, boolean up) {
        if (up) {
            var old = getById(entity.getId());
            if (!old.getParentId().equals(entity.getParentId())) {
                if (!parentIdIsNull(entity.getParentId())) {
                    var nowParent = getById(entity.getParentId());
                    entity.setPath(nowParent.getOPath());
                    if (nowParent.getLeaf()) {
                        nowParent.setLeaf(false);
                        BSService.super.update(nowParent);
                    }
                }
                if (!parentIdIsNull(old.getParentId())) {
                    var oldParent = getById(old.getParentId());
                    if (oldParent != null) {
                        var oldBrother = selectNextChildrenIds(oldParent.getId());
                        if (oldBrother.size() < 2) {
                            oldParent.setLeaf(true);
                            BSService.super.update(oldParent);
                        }
                    }
                }
            }
        }
        if (entity.getParentId() == null) {
            if (entity.getId() instanceof String) {
                entity.setParentId((ID) "0");
            } else if (entity.getId() instanceof Long) {
                entity.setParentId((ID) Long.valueOf(0));
            }
            entity.setPath("");
        }
        return entity;
    }

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


    /**
     * <p>
     * <h3>>作者 keray</h3>
     * <h3>>时间： 2019/8/16 16:08</h3>
     * 具有上下级的实体 设置树结构
     * </p>
     *
     * @param owner
     * @param n     获取多少级 -1 不限
     * @return <p>  </p>
     */
    default T setChildren(T owner, int n, Wrapper<T> inputQueryWrapper) {
        if (n == 0 || owner == null) {
            return owner;
        }
        owner.setChildren(CommonUtil.listData2Tree(allChildren(owner, n, inputQueryWrapper)));
        return owner;
    }

    // 获取所有下级id
    default List<ID> allChildrenIds(ID id, int n, Wrapper<T> inputQueryWrapper) {
        var owner = getById(id);
        if (owner == null) return new LinkedList<>();
        return allChildrenIds(owner, n, inputQueryWrapper);
    }

    default List<ID> allChildrenIds(ID id) {
        return allChildrenIds(id, -1, null);
    }

    default List<ID> allChildrenIds(T owner, int n, Wrapper<T> inputQueryWrapper) {
        if (n == 0 || owner == null) {
            return new LinkedList<>();
        }
        var ownerLevel = owner.getLevel();
        Wrapper<T> queryWrapper = inputQueryWrapperPath(owner.getOPath(), inputQueryWrapper, l -> l.select(T::getId), q -> q.select("id"));
        return getMapper().selectList(queryWrapper).stream().filter(v -> {
            if (n == -1) return true;
            var level = v.getLevel();
            return ownerLevel + n <= level;
        }).map(IBSEntity::getId).collect(Collectors.toList());
    }

    default List<ID> allChildrenIds(T owner) {
        return allChildrenIds(owner, -1, null);
    }


    default List<T> allChildren(ID id, int n, Wrapper<T> inputQueryWrapper) {
        return allChildren(getById(id), n, inputQueryWrapper);
    }

    default List<T> allChildren(ID id) {
        return allChildren(getById(id));
    }


    default List<T> allChildren(T owner, int n, Wrapper<T> inputQueryWrapper) {
        if (n == 0 || owner == null) {
            return new LinkedList<>();
        }
        Wrapper<T> queryWrapper = inputQueryWrapperPath(owner.getOPath(), inputQueryWrapper, null, null);
        var ownerLevel = owner.getLevel();
        return getMapper().selectList(queryWrapper).stream().filter(v -> {
            if (n == -1) return true;
            var level = v.getLevel();
            return ownerLevel + n <= level;
        }).map(this::modelDetail).collect(Collectors.toList());
    }

    default List<T> allChildren(T owner) {
        return allChildren(owner, -1, null);
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
        return selectList(wrappers().select(T::getId).eq(T::getParentId, id)).stream().map(T::getId).collect(Collectors.toList());
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
        return selectList(wrappers().eq(T::getParentId, id));
    }

    // 根据自身id获取父级
    default T parent(ID id, Wrapper<T> wrapper) {
        var m = selectOne(Wrappers.<T>lambdaQuery().select(T::getPath).eq(T::getId, id));
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
        var list = allParent(owner, inputQueryWrapper, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    // 根据自身查询所有父节点
    default List<T> allParent(T owner, Wrapper<T> inputQueryWrapper, int n) {
        var qids = allParentIds(owner, n);
        if (CollUtil.isEmpty(qids)) return new LinkedList<>();
        return selectList(inputQueryWrapper(inputQueryWrapper, l -> l.in(IBSEntity::getId, qids), q -> q.in("id", qids)));
    }

    default List<T> allParent(T owner, Wrapper<T> wrapper) {
        return allParent(owner, wrapper, -1);
    }

    default List<T> allParent(T owner) {
        return allParent(owner, null, -1);
    }

    // 根据自身id获取所有父级
    default List<T> allParent(ID id, Wrapper<T> inputQueryWrapper) {
        return allParent(getById(id), inputQueryWrapper, -1);
    }

    default List<T> allParent(ID id) {
        return allParent(id, null);
    }

    default List<ID> allParentIds(T owner, int n) {
        if (n == 0 || owner == null) return new LinkedList<>();
        var path = owner.getPath();
        var ids = path.split("/");
        n = n == -1 ? ids.length : n;
        List<ID> qids = new LinkedList<>();
        for (var i = 0; i < n; i++) {
            qids.add(owner.idCover(ids[ids.length - 1 - i]));
        }
        return qids;

    }

    default List<ID> allParentIds(T owner) {
        return allParentIds(owner, -1);
    }

    default List<ID> allParentIds(ID id, int n) {
        return allParentIds(getById(id), n);
    }

    default List<ID> allParentIds(ID id) {
        return allParentIds(id, -1);
    }

    // 根据父级id获取所有父级
    default List<ID> allParentIdsByParentId(ID parentId, int n) {
        var parent = getById(parentId);
        if (parent == null) return new LinkedList<>();
        parent.setPath(parent.getOPath());
        return allParentIds(parent, n);
    }

    default List<ID> allParentIdsByParentId(ID parentId) {
        return allParentIdsByParentId(parentId, -1);
    }

    // 根据父级id获取所有父级
    default List<T> allParentByParentId(ID parentId, Wrapper<T> inputQueryWrapper, int n) {
        var qids = allParentIdsByParentId(parentId, n);
        if (CollUtil.isEmpty(qids)) return new LinkedList<>();
        return selectList(inputQueryWrapper(inputQueryWrapper, l -> l.in(IBSEntity::getId, qids), q -> q.in("id", qids)));
    }

    default List<T> allParentByParentId(ID parentId, Wrapper<T> wrapper) {
        return allParentByParentId(parentId, wrapper, -1);
    }

    default List<T> allParentByParentId(ID parentId) {
        return allParentByParentId(parentId, null, -1);
    }

    default List<T> allTop(Wrapper<T> wrapper) {
        return selectList(inputQueryWrapper(wrapper,
                l -> l.eq(T::getPath, ""),
                q -> q.eq("path", ""))
        );
    }

    default List<T> allLeaf(Wrapper<T> wrapper) {
        return selectList(inputQueryWrapper(wrapper,
                l -> l.eq(T::getLeaf, true),
                q -> q.eq("leaf", true))
        );

    }

    default List<ID> allTopIds(Wrapper<T> wrapper) {
        return selectList(inputQueryWrapper(wrapper,
                l -> l.select(T::getId).eq(T::getPath, ""),
                q -> q.select("id").eq("path", ""))
        ).stream().map(T::getId).toList();
    }

    default List<ID> allLeafIds(Wrapper<T> wrapper) {
        return selectList(inputQueryWrapper(wrapper,
                l -> l.select(T::getId).eq(T::getLeaf, true),
                q -> q.select("id").eq("leaf", true))
        ).stream().map(T::getId).toList();
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


    default Wrapper<T> inputQueryWrapperPath(String path, Wrapper<T> inputQueryWrapper, Consumer<LambdaQueryWrapper<T>> call, Consumer<QueryWrapper<T>> call1) {
        if (path == null) {
            throw new BizRuntimeException("path 不能为null");
        }
        return inputQueryWrapper(inputQueryWrapper, l -> l.likeRight(T::getPath, path), q -> q.likeRight("path", path));
    }

    default Wrapper<T> inputQueryWrapper(Wrapper<T> inputQueryWrapper, Consumer<LambdaQueryWrapper<T>> call, Consumer<QueryWrapper<T>> call1) {
        if (inputQueryWrapper == null) {
            inputQueryWrapper = Wrappers.lambdaQuery();
        }
        Wrapper<T> queryWrapper = null;
        if (inputQueryWrapper instanceof QueryWrapper<T> q) {
            queryWrapper = q.clone();
            if (call1 != null) {
                call1.accept(((QueryWrapper<T>) queryWrapper));
            }
        }
        if (inputQueryWrapper instanceof LambdaQueryWrapper<T> q) {
            queryWrapper = q.clone();
            if (call != null) {
                call.accept(((LambdaQueryWrapper<T>) queryWrapper));
            }
        }
        if (queryWrapper == null) throw new BizRuntimeException();
        return queryWrapper;
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

}

