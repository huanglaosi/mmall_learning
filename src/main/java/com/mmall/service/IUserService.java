package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;

import javax.jws.soap.SOAPBinding;

public interface IUserService {

    //通过泛型做一个通用的数据响应对象
    ServerResponse<User> login(String username, String password);

    ServerResponse<String> register(User user);

    ServerResponse<String> checkValid(String str,String type);

    ServerResponse<String> selectQuestion(String username);

    public ServerResponse<String> checkAnswer(String username,String question,String answer);

    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken);

    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,User user);

    public ServerResponse<User> updateInformation(User user);

    public ServerResponse<User> getInformation(Integer userId);

    public ServerResponse checkAdminRole(User user);
}
