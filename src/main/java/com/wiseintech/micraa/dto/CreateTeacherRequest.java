package com.wiseintech.micraa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of POST /api/admin/teachers. Only an authenticated ADMIN may submit this request
 * (see AdminController). The role is never part of the payload: the endpoint itself is
 * the only thing that decides a TEACHER is created.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTeacherRequest {
    private String name;
    private String email;
    private String password;
}
