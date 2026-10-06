package com.davivienda.factoraje.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.davivienda.factoraje.dto.user.UserDTORequest;
import com.davivienda.factoraje.dto.user.UserDTOResponse;
import com.davivienda.factoraje.dto.user.UserUpdateDTORequest;

public interface UserService {

    Page<UserDTOResponse> getUsers(String search, Pageable pageable);

    UserDTOResponse createUser(UserDTORequest request);

    UserDTOResponse updateUser(UUID id, UserUpdateDTORequest request);

}
