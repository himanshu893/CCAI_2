package com.example.swaraj.SkyLock.Controllers;

import com.example.swaraj.SkyLock.Services.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class PageController {



    @GetMapping("/registerPage")
    public String registerPage() {
        return "register";   // returns register.html
    }

    @GetMapping("/loginPage")
    public String loginPage(Authentication authentication) {
        if(authentication != null && authentication.isAuthenticated()){
            return "redirect:/home";
        }
        return "login";      // returns login.html
    }

    @GetMapping("/uploadPage")
    public String uploadPage(){
        return "upload";
    }


}
