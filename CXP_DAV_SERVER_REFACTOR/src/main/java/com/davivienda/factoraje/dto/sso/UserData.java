package com.davivienda.factoraje.dto.sso;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserData {

    private String app;
    private String otc;
    private String user;
    private String userName;
    private String userEmail;
    private String companyName;
    private String companyUserNIU;
    private String status;
    private String timeStamp;
    
}
