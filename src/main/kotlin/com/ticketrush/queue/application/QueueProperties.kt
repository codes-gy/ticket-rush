package com.ticketrush.queue.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "queue")
class QueueProperties {
    /** 스케줄러가 한 번에 입장 허용시키는 인원 수. */
    var allowBatchSize: Int = 5

    /** 입장 허용 후, 좌석 선택 화면에 머물 수 있는 시간(분). 이 시간이 지나면 토큰이 만료되어 다시 대기열에 등록해야 한다. */
    var entryTokenTtlMinutes: Long = 5
}
