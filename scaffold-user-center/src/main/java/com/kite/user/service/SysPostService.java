package com.kite.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kite.user.entity.SysPost;
import com.kite.user.entity.SysDept;
import com.kite.user.entity.SysUserPost;
import com.kite.user.entity.SysPosition;
import com.kite.user.entity.SysEmployee;
import com.kite.user.mapper.SysPostMapper;
import com.kite.user.mapper.SysUserPostMapper;
import com.kite.user.mapper.SysDeptMapper;
import com.kite.user.mapper.SysPositionMapper;
import com.kite.user.mapper.SysEmployeeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysPostService {

    private final SysPostMapper postMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysDeptMapper deptMapper;
    private final SysPositionMapper positionMapper;
    private final SysEmployeeMapper employeeMapper;

    public SysPostService(SysPostMapper postMapper, SysUserPostMapper userPostMapper, SysDeptMapper deptMapper, SysPositionMapper positionMapper, SysEmployeeMapper employeeMapper) {
        this.postMapper = postMapper;
        this.userPostMapper = userPostMapper;
        this.deptMapper = deptMapper;
        this.positionMapper = positionMapper;
        this.employeeMapper = employeeMapper;
    }

    /**
     * 分页查询岗位
     */
    public IPage<SysPost> page(int page, int size, String keyword, Long deptId, Integer status) {
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysPost::getPostName, keyword)
                    .or().like(SysPost::getPostCode, keyword));
        }
        if (deptId != null) {
            wrapper.eq(SysPost::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(SysPost::getStatus, status);
        }
        wrapper.orderByAsc(SysPost::getSortOrder).orderByAsc(SysPost::getId);

        IPage<SysPost> result = postMapper.selectPage(new Page<>(page, size), wrapper);

        // 填充部门名称和在岗人数
        enrichPosts(result.getRecords());

        return result;
    }

    /**
     * 全量列表（下拉用）
     */
    public List<SysPost> list(Long deptId, Integer status) {
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(SysPost::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(SysPost::getStatus, status);
        }
        wrapper.orderByAsc(SysPost::getSortOrder).orderByAsc(SysPost::getId);
        List<SysPost> list = postMapper.selectList(wrapper);
        enrichPosts(list);
        return list;
    }

    /**
     * 按部门分组的岗位树
     */
    public List<Map<String, Object>> deptPostTree() {
        // 查所有部门
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSortOrder));

        // 查所有启用岗位
        List<SysPost> posts = postMapper.selectList(new LambdaQueryWrapper<SysPost>()
                .eq(SysPost::getStatus, 1)
                .orderByAsc(SysPost::getSortOrder));

        // 统计在岗人数
        Map<Long, Integer> userCountMap = getUserCountMap();

        // 按部门分组
        Map<Long, List<SysPost>> deptPostMap = posts.stream()
                .filter(p -> p.getDeptId() != null)
                .collect(Collectors.groupingBy(SysPost::getDeptId));

        List<SysPost> globalPosts = posts.stream()
                .filter(p -> p.getDeptId() == null)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();

        // 部门节点
        for (SysDept dept : depts) {
            List<SysPost> deptPosts = deptPostMap.get(dept.getId());
            if (deptPosts == null || deptPosts.isEmpty()) continue;

            Map<String, Object> deptNode = new LinkedHashMap<>();
            deptNode.put("key", "dept_" + dept.getId());
            deptNode.put("title", dept.getDeptName());
            deptNode.put("type", "dept");
            deptNode.put("children", deptPosts.stream().map(p -> {
                Map<String, Object> postNode = new LinkedHashMap<>();
                postNode.put("key", p.getId().toString());
                postNode.put("title", p.getPostName());
                postNode.put("type", "post");
                postNode.put("postCode", p.getPostCode());
                postNode.put("postCategory", p.getPostCategory());
                postNode.put("status", p.getStatus());
                postNode.put("userCount", userCountMap.getOrDefault(p.getId(), 0));
                return postNode;
            }).collect(Collectors.toList()));

            result.add(deptNode);
        }

        // 全局岗位
        if (!globalPosts.isEmpty()) {
            Map<String, Object> globalNode = new LinkedHashMap<>();
            globalNode.put("key", "global");
            globalNode.put("title", "全局岗位");
            globalNode.put("type", "global");
            globalNode.put("children", globalPosts.stream().map(p -> {
                Map<String, Object> postNode = new LinkedHashMap<>();
                postNode.put("key", p.getId().toString());
                postNode.put("title", p.getPostName());
                postNode.put("type", "post");
                postNode.put("postCode", p.getPostCode());
                postNode.put("postCategory", p.getPostCategory());
                postNode.put("status", p.getStatus());
                postNode.put("userCount", userCountMap.getOrDefault(p.getId(), 0));
                return postNode;
            }).collect(Collectors.toList()));
            result.add(globalNode);
        }

        return result;
    }

    /**
     * 新增岗位
     */
    public void create(SysPost post) {
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        postMapper.insert(post);
    }

    /**
     * 编辑岗位
     */
    public void update(SysPost post) {
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
    }

    /**
     * 删除岗位
     */
    @Transactional
    public void delete(Long id) {
        long positionCount = positionMapper.selectCount(new LambdaQueryWrapper<SysPosition>()
                .eq(SysPosition::getPostId, id));
        if (positionCount > 0) {
            throw new RuntimeException("该岗位下有职位，无法删除");
        }
        long employeeCount = employeeMapper.selectCount(new LambdaQueryWrapper<SysEmployee>()
                .eq(SysEmployee::getPostId, id));
        if (employeeCount > 0) {
            throw new RuntimeException("该岗位下有员工，无法删除");
        }
        postMapper.deleteById(id);
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getPostId, id));
    }

    /**
     * 查用户岗位ID
     */
    public List<Long> getUserPostIds(Long userId) {
        return userPostMapper.selectPostIdsByUserId(userId);
    }

    /**
     * 分配用户岗位
     */
    @Transactional
    public void assignUserPosts(Long userId, List<Long> postIds) {
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        if (postIds != null) {
            for (Long postId : postIds) {
                userPostMapper.insert(new SysUserPost(userId, postId));
            }
        }
    }

    /**
     * 根据岗位ID查询职位列表
     */
    public List<SysPosition> getPositionsByPostId(Long postId) {
        return positionMapper.selectList(new LambdaQueryWrapper<SysPosition>()
                .eq(SysPosition::getPostId, postId)
                .orderByAsc(SysPosition::getSortOrder));
    }

    // ============ 私有方法 ============

    private void enrichPosts(List<SysPost> posts) {
        if (posts == null || posts.isEmpty()) return;

        Set<Long> deptIds = posts.stream()
                .map(SysPost::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> deptNameMap = Collections.emptyMap();
        if (!deptIds.isEmpty()) {
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            deptNameMap = depts.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        }

        Map<Long, Integer> userCountMap = getUserCountMap();

        Set<Long> postIds = posts.stream().map(SysPost::getId).collect(Collectors.toSet());
        List<SysPosition> allPositions = positionMapper.selectList(new LambdaQueryWrapper<SysPosition>()
                .in(SysPosition::getPostId, postIds)
                .orderByAsc(SysPosition::getSortOrder));
        Map<Long, List<SysPosition>> positionMap = allPositions.stream()
                .collect(Collectors.groupingBy(SysPosition::getPostId));

        for (SysPost post : posts) {
            if (post.getDeptId() != null) {
                post.setDeptName(deptNameMap.getOrDefault(post.getDeptId(), ""));
            } else {
                post.setDeptName("全局");
            }
            post.setUserCount(userCountMap.getOrDefault(post.getId(), 0));
            post.setPositions(positionMap.getOrDefault(post.getId(), Collections.emptyList()));
        }
    }

    private Map<Long, Integer> getUserCountMap() {
        List<Map<String, Object>> counts = postMapper.countUsersByPost();
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : counts) {
            Long postId = ((Number) row.get("post_id")).longValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            map.put(postId, cnt);
        }
        return map;
    }
}
