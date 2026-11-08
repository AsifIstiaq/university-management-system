package com.example.university.service;

import com.example.university.dto.UserRequest;
import com.example.university.entity.Admin;
import com.example.university.exception.ResourceNotFoundException;
import com.example.university.repository.AdminRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final AdminRepository adminRepository;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    public AdminService(AdminRepository adminRepository, AccountService accountService, ModelMapper modelMapper) {
        this.adminRepository = adminRepository;
        this.accountService = accountService;
        this.modelMapper = modelMapper;
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id " + id));
    }

    public long count() {
        return adminRepository.count();
    }

    @Transactional
    public Admin saveAdmin(UserRequest request) {
        Admin admin = modelMapper.map(request, Admin.class);
        accountService.initializeNewAccount(admin, request);
        return adminRepository.save(admin);
    }

    @Transactional
    public Admin updateAdmin(Long id, UserRequest request) {
        Admin admin = getAdminById(id);
        accountService.applyProfile(admin, request);
        return adminRepository.save(admin);
    }

    @Transactional
    public void deleteAdmin(Long id) {
        Admin admin = getAdminById(id);
        accountService.deleteAccount(admin.getId());
    }
}
