package com.wavesenterprise.we.tx.observer.core.spring.web.service

import com.wavesenterprise.we.tx.observer.domain.RollbackInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface RollbackInfoService {
    fun list(pageable: Pageable): Page<RollbackInfo>
    fun count(): Long
}
