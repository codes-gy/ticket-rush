package com.ticketrush.concert.infrastructure

import com.ticketrush.concert.domain.Concert
import org.springframework.data.jpa.repository.JpaRepository

interface ConcertRepository : JpaRepository<Concert, Long>
