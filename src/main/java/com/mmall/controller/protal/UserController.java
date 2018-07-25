package com.mmall.controller.protal;

import com.mmall.common.Const;
import com.mmall.common.RedisPool;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisPoolUtil;
import net.sf.jsqlparser.schema.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.UnknownServiceException;

@Controller
@RequestMapping("/user/")
public class UserController {

    @Autowired
    private IUserService iUserService;

    /**
     * 用户登陆
     * @param username
     * @param password
     * @param session
     * @return
     */
    /**当涉及到数据库增删查改是根据数据库主键进行的时候，就要充分考虑到横向越权问题
     * 所以要有userId的存在进行检验
     * 判断所需要操作的信息是否属于对应的用户
     * 在使用 @RequestMapping 后，返回值通常解析为跳转路径，加上 @Responsebody 后返回结果不会被解析为跳转路径，而是直接写入HTTP     响应正文中。例如，异步获取 json 数据，加上 @Responsebody 注解后，就会直接返回 json 数据。
     * @RequestBody 注解则是将 HTTP 请求正文插入方法中，使用适合的 HttpMessageConverter 将请求体写入某个对象。
     */
    @RequestMapping(value="login.do",method = RequestMethod.POST)
    //返回时自动通过spring-mvc的jackson插件，将返回值序列化成json
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session, HttpServletResponse httpServletResponse, HttpServletRequest httpServletRequest){
        //service-->mybatis-->dao
        ServerResponse<User> response=iUserService.login(username,password);
        if(response.isSuccess()){


//            session.setAttribute(Const.CURRENT_USER,response.getData());
           // E21B9B8813E679D8B844049C01A0F6D3
           // E21B9B8813E679D8B844049C01A0F6D3
            CookieUtil.writeLoginToken(httpServletResponse,session.getId());
            CookieUtil.readLoginToken(httpServletRequest);
            CookieUtil.delLoginToken(httpServletRequest,httpServletResponse);
            RedisPoolUtil.setEx(session.getId(), JsonUtil.obj2String(response.getData()),Const.RedisCacheExtime.REDIS_SESSION_EXTIME);

        }
        //此时的response已经被序列化为json格式返回
        return response;
    }

    @RequestMapping(value = "logout.do",method =RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session){
        session.removeAttribute(Const.CURRENT_USER);
        //返回响应对象response
        return ServerResponse.createBySuccess();
    }

    @RequestMapping(value = "register.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user){
        //在注册这里验证是为了防止恶意调用注册接口
        return iUserService.register(user);
    }

    @RequestMapping(value = "check_valid.do",method = RequestMethod.POST)
    @ResponseBody
    //前端在注册姓名，点击下一个input时,要实时调用一个校验接口，在前台实时反馈（AJAX异步调用）
    //type用来判断是username还是email，str是value值
    public ServerResponse<String> checkValid(String str,String type){
        return iUserService.checkValid(str,type);
    }


    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session){
        User user =(User) session.getAttribute(Const.CURRENT_USER);
        if(user!=null){
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
    }

    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username){
        return iUserService.selectQuestion(username);
    }

    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer){
        return  iUserService.checkAnswer(username,question,answer);
    }

    //忘记密码的重置密码
    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetRestPassword(String username,String passwordNew,String forgetToken){
        return iUserService.forgetRestPassword(username,passwordNew,forgetToken);
    }

    @RequestMapping(value ="reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    //登陆状态的重置密码
    public ServerResponse<String> resetpassword(HttpSession session,String passwordOld,String passwordNew){
        User user=(User)session.getAttribute(Const.CURRENT_USER);
        if(user==null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.resetPassword(passwordOld,passwordNew,user);
    }

    @RequestMapping(value ="update_information.do",method = RequestMethod.POST)
    @ResponseBody
   public ServerResponse<User> update_information(HttpSession session,User user){
        User currentUser=(User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser==null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }

        //user从前端传递过来时无userid，防止越权，从前端传过来的id被改变，从而改变别的user数据
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());
        //将返回的updateuser放入response对象中
        ServerResponse<User> response=iUserService.updateInformation(user);
        if(response.isSuccess()){
            response.getData().setUsername(currentUser.getUsername());
            session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
   }

    @RequestMapping(value ="get_information.do",method = RequestMethod.POST)
    @ResponseBody
   //在个人中心修改个人信息的时候，首先要get_information,然后再update_information
   public ServerResponse<User> get_information(HttpSession session){
       //随着业务扩展，user需要的会可能变多。session里我们只会放这些，不会再增加。和未来的扩展解耦
       //session存的数据不一定与用户表中的user一一对应
       User currentUser=(User)session.getAttribute(Const.CURRENT_USER);
       if(currentUser==null){
           //跟前端约定传10则要强制登陆
           return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，需要强制登陆status=10");
       }
       return iUserService.getInformation(currentUser.getId());
   }
}
