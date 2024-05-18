package com.ariel.findfriendbackend.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 *加入队伍参数
 * @author Ariel
 */
@Data
public class TeamJoinRequest implements Serializable {
    /**
     * id
     */
    private Long TeamId;

    private String currentId;

    /**
     * 密码
     *
     */
    private String password;
}
