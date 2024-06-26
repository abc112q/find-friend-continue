package com.ariel.findfriendbackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息封装类
 * @author Ariel
 */
@Data
public class CurrentUserVO implements Serializable {

    private static final long serialVersionUID = 1899063007109226944L;

    /**
     * id
     */
    private Long id;


}
