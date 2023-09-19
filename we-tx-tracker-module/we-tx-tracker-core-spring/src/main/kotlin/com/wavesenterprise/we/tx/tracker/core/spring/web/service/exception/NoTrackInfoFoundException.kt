package com.wavesenterprise.we.tx.tracker.core.spring.web.service.exception

class NoTrackInfoFoundException(id: String) : IllegalArgumentException("Cannot find track info by id: $id")
