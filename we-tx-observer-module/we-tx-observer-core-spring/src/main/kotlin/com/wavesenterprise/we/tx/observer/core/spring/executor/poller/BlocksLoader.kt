package com.wavesenterprise.we.tx.observer.core.spring.executor.poller

import com.wavesenterprise.sdk.node.domain.blocks.BlockAtHeight

interface BlocksLoader {
    fun download(fromHeight: Long, tryToHeight: Long): BlocksDownloadResult
}

data class BlocksDownloadResult(
    val blocks: List<BlockAtHeight>,
    val moreBlocksExist: Boolean,
) {
    companion object {
        val NO_BLOCKS = BlocksDownloadResult(
            blocks = emptyList(),
            moreBlocksExist = false,
        )
    }
}
