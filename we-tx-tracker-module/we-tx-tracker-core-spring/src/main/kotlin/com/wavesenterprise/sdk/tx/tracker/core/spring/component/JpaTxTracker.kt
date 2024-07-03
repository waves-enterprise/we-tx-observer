package com.wavesenterprise.sdk.tx.tracker.core.spring.component

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.client.feign.tx.mapDto
import com.wavesenterprise.sdk.node.client.http.tx.TxDto
import com.wavesenterprise.sdk.node.client.http.tx.TxDto.Companion.toDomain
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.contract.ContractId
import com.wavesenterprise.sdk.node.domain.contract.ContractTxStatus
import com.wavesenterprise.sdk.node.domain.contract.ContractTxStatus.Companion.FATAL_ERROR_CODE
import com.wavesenterprise.sdk.node.domain.contract.TxStatus
import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.sdk.node.domain.tx.CreateContractTx
import com.wavesenterprise.sdk.node.domain.tx.DisableContractTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.Tx.Companion.type
import com.wavesenterprise.sdk.node.domain.tx.UpdateContractTx
import com.wavesenterprise.sdk.tx.tracker.api.TxTracker
import com.wavesenterprise.sdk.tx.tracker.core.spring.properties.TxTrackerProps
import com.wavesenterprise.sdk.tx.tracker.domain.SmartContractInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackBusinessObjectInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.BusinessObjectInfoJpaRepository
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.SmartContractInfoJpaRepository
import com.wavesenterprise.sdk.tx.tracker.jpa.repository.TxTrackerJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

open class JpaTxTracker(
    val txTrackerJpaRepository: TxTrackerJpaRepository,
    val smartContractInfoJpaRepository: SmartContractInfoJpaRepository,
    val businessObjectInfoJpaRepository: BusinessObjectInfoJpaRepository,
    val objectMapper: ObjectMapper = jacksonObjectMapper(),
    val txService: TxService,
    val txTrackerProperties: TxTrackerProps,
) : TxTracker {

    private val findContractInNode = txTrackerProperties.findContractInNode
    private val minContractTxErrorCount = txTrackerProperties.minContractTxErrorCount
    private val minContractTxFailureCount = txTrackerProperties.minContractTxFailureCount
    private val failOnRecoverableContractTxError = txTrackerProperties.failOnRecoverableContractTxError

    @Transactional
    override fun trackTx(
        tx: Tx,
        meta: Map<String, Any>,
        businessObjectInfos: List<TxTrackBusinessObjectInfo>,
        userId: String?
    ): TxTrackInfo =
        when (tx) {
            is CreateContractTx -> smartContractInfoJpaRepository.save(
                SmartContractInfo(
                    id = tx.id.asBase58String(),
                    sender = tx.senderAddress.asBase58String(),
                    image = tx.image.value,
                    imageHash = tx.imageHash.value,
                    contractName = tx.contractName.value,
                    version = tx.version.value,
                )
            )
            is CallContractTx -> getSmartContractInfo(tx.contractId, tx.javaClass.simpleName)
            is DisableContractTx -> getSmartContractInfo(tx.contractId, tx.javaClass.simpleName)
            is UpdateContractTx -> getSmartContractInfo(tx.contractId, tx.javaClass.simpleName)
            else -> null
        }.let { smartContractInfo ->
            txTrackerJpaRepository.save(
                TxTrackInfo(
                    id = tx.id.asBase58String(),
                    type = tx.type().code,
                    body = objectMapper.valueToTree(mapDto(tx)),
                    smartContractInfo = smartContractInfo,
                    meta = meta,
                    businessObjectInfos = businessObjectInfos,
                    userId = userId
                )
            )
        }

    private fun getSmartContractInfo(contractId: ContractId, txTypeName: String) =
        smartContractInfoJpaRepository.findByIdOrNull(contractId.asBase58String()) ?: if (findContractInNode) {
            findAndSaveContractFromNode(contractId.asBase58String())
        } else {
            throw IllegalArgumentException("No smartContractInfo for tracked $txTypeName [contractId = '$contractId']")
        }

    override fun existsInTracker(tx: Tx): Boolean = txTrackerJpaRepository.existsById(tx.id.asBase58String())

    override fun existsInTrackerWithStatus(tx: Tx, txTrackerStatus: TxTrackStatus): Boolean =
        txTrackerJpaRepository.existsByIdAndStatus(tx.id.asBase58String(), txTrackerStatus)

    override fun getTrackedTxWithStatus(txTrackerStatus: TxTrackStatus): List<Tx> =
        txTrackerJpaRepository.findAllByStatus(txTrackerStatus).map {
            objectMapper.treeToValue(it.body, TxDto::class.java)
        }.map { it.toDomain() }

    override fun getTrackedTxIdsWithStatus(txTrackerStatus: TxTrackStatus): List<TxId> =
        txTrackerJpaRepository.findAllByStatus(txTrackerStatus).map {
            TxId.fromBase58(it.id)
        }

    override fun getTrackedTxIdsWithStatusAndTypes(
        txTrackerStatus: TxTrackStatus,
        types: List<Int>,
        pageRequest: PageRequest,
    ): List<TxId> = txTrackerJpaRepository.findLastIdsByStatusAndTypeIn(
        txTrackerStatus, types, pageRequest
    ).map { TxId.fromBase58(it) }

    override fun getTrackedTxsWithStatus(
        txTrackerStatus: TxTrackStatus,
        pageRequest: PageRequest,
    ): List<Tx> = txTrackerJpaRepository.findLastBodiesByStatus(
        txTrackerStatus, pageRequest
    ).mapNotNull { body: JsonNode? ->
        body?.let { objectMapper.convertValue<TxDto>(it).toDomain() }
    }

    override fun getLastUnsuccessfulTrackedTxForBusinessObject(
        businessObjectId: String,
    ): Optional<TxTrackInfo> =
        txTrackerJpaRepository.findFirstByStatusNotAndBusinessObjectInfosContainsOrderByModifiedDesc(
            status = TxTrackStatus.SUCCESS,
            businessObjectInfos = businessObjectInfoJpaRepository.getReferenceById(businessObjectId),
        )

    override fun getLastTrackedTxForBusinessObjectWithStatus(
        businessObjectId: String,
        status: TxTrackStatus,
    ): Optional<TxTrackInfo> =
        txTrackerJpaRepository.findFirstByStatusAndBusinessObjectInfosContainsOrderByModifiedDesc(
            status = status,
            businessObjectInfos = businessObjectInfoJpaRepository.getReferenceById(businessObjectId),
        )

    @Transactional
    override fun setContractTxError(txId: TxId, contractTxStatusDtoList: List<ContractTxStatus>) {
        txTrackerJpaRepository.findById(txId.asBase58String()).ifPresent { trackInfo ->
            trackInfo.setStatus(contractTxStatusDtoList)
        }
    }

    private fun TxTrackInfo.setStatus(contractTxStatusDtoList: List<ContractTxStatus>) {
        errors = objectMapper.valueToTree(contractTxStatusDtoList)
        if (contractTxStatusDtoList.isEmpty()) {
            return
        }
        val statusMap = contractTxStatusDtoList.groupBy { it.status }
        status = when {
            statusMap.containsKey(TxStatus.SUCCESS) -> status
            (statusMap[TxStatus.ERROR]?.errorToFailCount() ?: 0) >= minContractTxErrorCount -> TxTrackStatus.ERROR
            (statusMap[TxStatus.FAILURE]?.count() ?: 0) >= minContractTxFailureCount -> TxTrackStatus.FAILURE
            else -> status
        }
    }

    private fun List<ContractTxStatus>.errorToFailCount(): Int =
        if (failOnRecoverableContractTxError) {
            count()
        } else {
            count { it.code == FATAL_ERROR_CODE }
        }

    @Transactional
    override fun setTrackStatus(tx: Tx, status: TxTrackStatus) {
        txTrackerJpaRepository.findById(tx.id.asBase58String()).apply {
            if (isPresent) {
                get().status = status
            }
        }
    }

    private fun findAndSaveContractFromNode(contractId: String) =
        (txService.txInfo(TxId.fromBase58(contractId)).get().tx as CreateContractTx).let {
            smartContractInfoJpaRepository.save(
                SmartContractInfo(
                    id = it.id.asBase58String(),
                    sender = it.senderAddress.asBase58String(),
                    image = it.image.value,
                    imageHash = it.imageHash.value,
                    contractName = it.contractName.value,
                    version = it.version.value,
                )
            )
        }
}
