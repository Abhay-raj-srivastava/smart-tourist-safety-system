package com.epic.touristsafety.entity;

public enum Role {
    USER,       // Default base role
    TOURIST,    // Specifically for registered tourists
    AUTHORITY,  // Police, Hospitals, Admins
    ADMIN       // System administrators
}