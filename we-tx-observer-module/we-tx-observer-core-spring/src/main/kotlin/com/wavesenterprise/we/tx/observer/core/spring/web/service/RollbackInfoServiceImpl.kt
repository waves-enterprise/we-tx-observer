package com.wavesenterprise.we.tx.observer.core.spring.web.service

import com.wavesenterprise.we.tx.observer.domain.RollbackInfo
import com.wavesenterprise.we.tx.observer.jpa.repository.RollbackInfoRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class RollbackInfoServiceImpl(
    private val rollbackInfoRepository: RollbackInfoRepository,
) : RollbackInfoService {
    override fun list(pageable: Pageable): Page<RollbackInfo> =
        rollbackInfoRepository.findAll(
            { _, _, _ -> null },
            pageable
        )

    override fun count(): Long =
        rollbackInfoRepository.count()
}
