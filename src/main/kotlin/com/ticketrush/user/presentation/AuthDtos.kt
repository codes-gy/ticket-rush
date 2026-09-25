package com.ticketrush.user.presentation

import com.ticketrush.user.domain.User
import com.ticketrush.user.domain.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,
)

data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    val password: String,
)

data class TokenResponse(
    val accessToken: String,
)

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: UserRole,
) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(id = requireNotNull(user.id), email = user.email, name = user.name, role = user.role)
    }
}
