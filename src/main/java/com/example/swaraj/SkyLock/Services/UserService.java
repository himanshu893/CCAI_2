package com.example.swaraj.SkyLock.Services;

import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Repo.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UsersRepo repo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTServices jwtServices;
    @Autowired
    private FileService fileService;

    public UserService(
            UsersRepo repo,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JWTServices jwtServices) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtServices = jwtServices;
    }

    public Users registers(Users user) {
        if (repo.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        repo.save(user);
        fileService.createUserStroageFolder(user.getId());
        return user;
    }

    public String verify(Users user) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
            return jwtServices.genrateToken(user.getUsername());
        } catch (BadCredentialsException exception) {
            throw new AuthenticationServiceException("Invalid username or password", exception);
        }
    }

//    public boolean isAlreadyLogin(){
//
//    }

    public List<Users> findAllUser() {
        return repo.findAll();
    }

    //

}
