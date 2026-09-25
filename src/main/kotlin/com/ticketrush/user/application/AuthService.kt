package com.ticketrush.user.application

import com.ticketrush.common.exception.ConflictException
import com.ticketrush.common.exception.UnauthorizedException
import com.ticketrush.common.security.JwtTokenProvider
import com.ticketrush.user.domain.User
import com.ticketrush.user.infrastructure.UserRepository
import com.ticketrush.user.presentation.LoginRequest
import com.ticketrush.user.presentation.SignUpRequest
import com.ticketrush.user.presentation.TokenResponse
import com.ticketrush.user.presentation.UserResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun signUp(request: SignUpRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("이미 가입된 이메일입니다.")
        }

        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            name = request.name,
        )
        return UserResponse.from(userRepository.save(user))
    }

    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        val accessToken = jwtTokenProvider.createAccessToken(requireNotNull(user.id), user.role.name)
        return TokenResponse(accessToken)
    }
}
