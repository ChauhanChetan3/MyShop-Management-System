package com.inn.shop.POJO;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@NamedQuery(name = "User.findByEmailId", query = "Select u from User u where u.email=:email")
@NamedQuery(name = "User.getAllUser", query = "select new com.inn.shop.wrapper.UserWrapper(u.id,u.name,u.email,u.contactNumber,u.status) from User u where u.role='user'")
@NamedQuery(name = "User.updateStatus", query = "update User u set u.status=:status where u.id=:id")
@NamedQuery(name = "User.getAllAdmin", query = "select u.email from User u where u.role='admin'")




@Data
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name= "user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Integer id;
    @Column(name="name")
    private String name;

    @Column(name="contactNumber")
    private String contactNumber;
    @Column(name="email")
    private String email;
    @Column(name="password")
    private String password;

    @Column(name="status")
    private String status;
    @Column(name="role")
    private String role;



}
