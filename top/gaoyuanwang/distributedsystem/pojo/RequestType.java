package top.gaoyuanwang.distributedsystem.pojo;

import java.io.Serializable;

/**
 * the type of request, REQUEST_ASSIGN_SLAVE will need to carry the master port
 * the last two types are the response of REQUEST_PING and REQUEST_ASSIGN_SLAVE respectively
 */
public enum RequestType implements Serializable {
    REQUEST_GAME_INFO, REQUEST_PING, REQUEST_ASSIGN_SLAVE, REQUEST_PONG, REQUEST_SLAVE_ASSIGNED;

    public Integer masterPort;
}
