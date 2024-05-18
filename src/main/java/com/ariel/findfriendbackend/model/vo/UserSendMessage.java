package com.ariel.findfriendbackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮件发送验证码
 * @author Ariel
 */
@Data
public class UserSendMessage implements Serializable {

    private static final long serialVersionUID = 46412442243484364L;

    private String userEmail;
    private String code;

}