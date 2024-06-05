package com.wavesenterprise.sdk.tx.tracker.read.starter.web.service.exception

class NoTrackInfoFoundException(id: String) : IllegalArgumentException("Cannot find track info by id: $id")
