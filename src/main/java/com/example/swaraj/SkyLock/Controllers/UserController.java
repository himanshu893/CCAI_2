package com.example.swaraj.SkyLock.Controllers;

import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Services.JWTServices;
import com.example.swaraj.SkyLock.Services.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class UserController {

    private final UserService service;
    @Autowired
    private JWTServices jwtServices;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute Users user) {
        try {
            service.registers(user);
        } catch (Exception e) {
            return "redirect:/registerPage?error";
        }
        return "redirect:/loginPage";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute Users user, HttpServletResponse response, HttpServletRequest request) {

        String token;
        try {
            token = service.verify(user);
        } catch (Exception e) {
            return "redirect:/loginPage?error";
        }

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);
        response.addCookie(cookie);

        return "redirect:/home";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {

        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // delete
        response.addCookie(cookie);

        return "redirect:/loginPage";
    }

    @GetMapping("/users")
    public String showUsers(Model model) {

        List<Users> users = service.findAllUser();
        model.addAttribute("users", users);

        return "users";
    }

    @GetMapping("/home")
    public String home(Model model){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("username",username);
        return "home";
    }

}
