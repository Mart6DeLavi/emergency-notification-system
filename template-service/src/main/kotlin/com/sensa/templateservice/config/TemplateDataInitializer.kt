package com.sensa.templateservice.config

import com.sensa.templateservice.entity.Template
import com.sensa.templateservice.entity.TemplateChannel
import com.sensa.templateservice.repository.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("!test")
class TemplateDataInitializer(
    private val templateRepository: TemplateRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(TemplateDataInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        if (!templateRepository.existsByTemplateName(EMERGENCY_ALERT)) {
            templateRepository.save(
                Template(
                    templateName = EMERGENCY_ALERT,
                    description = "Emergency alert broadcast template",
                    channel = TemplateChannel.EMAIL,
                    content = EMERGENCY_ALERT_CONTENT,
                    createdBy = SYSTEM_USER_ID
                )
            )
            log.info("Seeded default template '{}'", EMERGENCY_ALERT)
        }
    }

    companion object {
        private const val EMERGENCY_ALERT = "emergency-alert"
        private val SYSTEM_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val EMERGENCY_ALERT_CONTENT = """
            ⚠️ Emergency alert
            {{title}}
            {{description}}
            Location: {{city}}, {{street}}
        """.trimIndent()
    }
}
