package com.ssafy.domain.usecase.login

import com.ssafy.domain.model.login.resp.CheckBirth
import com.ssafy.domain.repository.LoginRepository
import javax.inject.Inject

class CheckKakaoTokenUsecase @Inject constructor(private val repository: LoginRepository) {
    suspend operator fun invoke(token: String): CheckBirth {
       return repository.checkKakaoToken(token)
    }
}