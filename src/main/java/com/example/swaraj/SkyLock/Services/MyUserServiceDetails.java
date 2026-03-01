package com.example.swaraj.SkyLock.Services;

import com.example.swaraj.SkyLock.Models.UserPrincipal;
import com.example.swaraj.SkyLock.Models.Users;
import com.example.swaraj.SkyLock.Repo.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserServiceDetails implements UserDetailsService {

    private UsersRepo repo;

    public MyUserServiceDetails(UsersRepo repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = repo.findByUsername(username);
        if(user == null){
            System.out.println("User is Not found");
            throw new UsernameNotFoundException("User is not found");
        }
        System.out.println("Success");
        return new UserPrincipal(user);
    }
}
