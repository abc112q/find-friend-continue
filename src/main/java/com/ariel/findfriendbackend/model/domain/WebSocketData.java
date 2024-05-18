package com.ariel.findfriendbackend.model.domain;

import lombok.Data;

import javax.websocket.Session;

@Data
public class WebSocketData {
    /**
     * 当前连接
     */
    private Session session;

    /**
     * 当前通讯Id
     */

    private String communicationId;
}
