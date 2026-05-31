package com.github.paicoding.forum.service.user.service;

/**
 * @author YiHui
 * 创建于 2025/9/29
 */
public interface UserTransferService {

    boolean transferUser(String uname, String pwd);

    boolean transferUser(String starNumber);
}
