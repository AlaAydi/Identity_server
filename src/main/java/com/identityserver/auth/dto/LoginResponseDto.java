package com.identityserver.auth.dto;

import com.identityserver.user.dto.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserResponseDto user;
}
