package com.kite.user.entity;

import com.baomidou.mybatisplus.annotation.*;

@TableName("sys_user_post")
public class SysUserPost {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;

    public SysUserPost() {}
    public SysUserPost(Long userId, Long postId) {
        this.userId = userId;
        this.postId = postId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
}
