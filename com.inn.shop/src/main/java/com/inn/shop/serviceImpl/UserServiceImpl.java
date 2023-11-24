package com.inn.shop.serviceImpl;

import com.google.common.base.Strings;
import com.inn.shop.JWT.CustomerUsersDetailsService;
import com.inn.shop.JWT.JwtFilter;
import com.inn.shop.JWT.JwtUtil;
import com.inn.shop.POJO.User;
import com.inn.shop.constants.MyShopConstants;
import com.inn.shop.dao.UserDao;
import com.inn.shop.service.UserService;
import com.inn.shop.utils.EmailUtils;
import com.inn.shop.utils.MyShopUtils;
import com.inn.shop.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j //login purpose
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    CustomerUsersDetailsService customerUsersDetailsService;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    JwtFilter jwtFilter;
    @Autowired
    EmailUtils emailUtils;

    @Override
    public ResponseEntity<String> signUp(Map<String, String> requestMap) {
       log.info("Inside signup {}", requestMap);
     try{
       if(validateSignUpMap(requestMap)){
           User user = userDao.findByEmailId(requestMap.get("email"));
           if(Objects.isNull(user)){
               userDao.save(getUserFromMap(requestMap));
               return MyShopUtils.getResponseEntity("Successfully Resistered.", HttpStatus.OK);
           }else {
               return MyShopUtils.getResponseEntity("Email already exist.",HttpStatus.BAD_REQUEST);
           }
       }else{
           return MyShopUtils.getResponseEntity(MyShopConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);

       }
     }catch (Exception ex){
         ex.printStackTrace();
     }
     return  MyShopUtils.getResponseEntity(MyShopConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);

    }

    private boolean validateSignUpMap(Map<String, String> requestMap){
        if(requestMap.containsKey("name") && requestMap.containsKey("contactNumber") &&
        requestMap.containsKey("email") && requestMap.containsKey("password") ) {
            return true;
        }else {
            return false;
        }
    }



    private User getUserFromMap(Map<String, String> requestMap){
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setStatus("false");
        user.setRole("user");
        return user;
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"),requestMap.get("password"))
            );
            if(auth.isAuthenticated()){
                if(customerUsersDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")){
                    return new ResponseEntity<String>("{\"token\":\""+
                            jwtUtil.generateToken(customerUsersDetailsService.getUserDetail().getEmail(),
                            customerUsersDetailsService.getUserDetail().getRole()) + "\"}",
                    HttpStatus.OK);
                }else {
                    return new ResponseEntity<String>("{\"message\":\""+"Wait for admin approval."+"\"}",
                            HttpStatus.BAD_REQUEST);
                }
            }
        }catch (Exception ex){
            log.error("{}",ex);
        }
        return new ResponseEntity<String>("{\"message\":\""+"Bad Credentials."+"\"}",
                HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
           if(jwtFilter.isAdmin()){
               return new ResponseEntity<>(userDao.getAllUser(),HttpStatus.OK);
           }else {
               return new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);
           }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if(jwtFilter.isAdmin()){
              Optional<User> optional = userDao.findById(Integer.parseInt(requestMap.get("id")));
              if(!optional.isEmpty()){
                 userDao.updateStatus(requestMap.get("status"),Integer.parseInt(requestMap.get("id")));
                 sendMailToAllAdmin(requestMap.get("status"),optional.get().getEmail(),userDao.getAllAdmin());
                  return MyShopUtils.getResponseEntity("User Status Updated Successfully",HttpStatus.OK);
              }else {
                  return MyShopUtils.getResponseEntity("User id doesn't exist", HttpStatus.OK);
              }
            }else {
                return MyShopUtils.getResponseEntity(MyShopConstants.UNAUTHORIZED_ACCESS,HttpStatus.UNAUTHORIZED);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return MyShopUtils.getResponseEntity(MyShopConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }



    private void sendMailToAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if (status != null && status.equalsIgnoreCase("true")){
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account Approved", "USER:- "+user+"\n is approved by \nADMIN:-"+ jwtFilter.getCurrentUser(), allAdmin);
        }else {
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account Disabled", "USER:- "+user+"\n is disabled by \nADMIN:-"+ jwtFilter.getCurrentUser(), allAdmin);
        }
    }

    @Override
    public ResponseEntity<String> checkToken() {
        return MyShopUtils.getResponseEntity("true",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            User userObj = userDao.findByEmail(jwtFilter.getCurrentUser());
            if(!userObj.equals(null)){
                if(userObj.getPassword().equals(requestMap.get("oldPassword"))){
                    userObj.setPassword(requestMap.get("newPassword"));
                    userDao.save(userObj);
                    return MyShopUtils.getResponseEntity("Password Updated Successfully",HttpStatus.OK);
                }
                return MyShopUtils.getResponseEntity("Incorrect Old Password", HttpStatus.BAD_REQUEST);
            }
              return MyShopUtils.getResponseEntity(MyShopConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return MyShopUtils.getResponseEntity(MyShopConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            User user = userDao.findByEmail(requestMap.get("email"));
            if(!Objects.isNull(user) && !Strings.isNullOrEmpty(user.getEmail()))
                emailUtils.forgotMail(user.getEmail(),"Credentials By MyShop Management System", user.getPassword());
            return MyShopUtils.getResponseEntity("Check your mail for Credentials",HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return MyShopUtils.getResponseEntity(MyShopConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
