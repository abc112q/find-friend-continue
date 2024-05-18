package com.ariel.findfriendbackend.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Ariel
 */
@Data
public class TeamUpdateRequest implements Serializable {

    private String currentId;
    /**
     * id
     */
    private Long id;
    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    private String avatarUrl;
    /**
     * 密码
     */
    private String password;
}
