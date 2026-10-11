// Post-actions — notification hooks.
//
// Notifications are intentionally NOT sent yet. Everything is wired so that an
// email can be plugged in later by setting NOTIFY_ENABLED=true and filling
// NOTIFY_EMAIL_RECIPIENTS (requires the Email Extension plugin for `emailext`).

def notify(String result) {
    def enabled = (env.NOTIFY_ENABLED ?: 'false').toString().toBoolean()
    def recipients = env.NOTIFY_EMAIL_RECIPIENTS ?: ''

    if (enabled) {
        if (!recipients) {
            echo 'NOTIFY_ENABLED=true but NOTIFY_EMAIL_RECIPIENTS is empty; skipping email.'
            return
        }
        // Ready-to-use hook — uncomment once the Email Extension plugin is installed:
        //
        // emailext(
        //     to: recipients,
        //     subject: "[SENSA] ${result}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        //     body: """
        //         Pipeline result: ${result}
        //         Job: ${env.JOB_NAME} #${env.BUILD_NUMBER}
        //         Build URL: ${env.BUILD_URL}
        //     """
        // )
        echo "Notification hook enabled for ${recipients} (email sending is commented out)."
    } else {
        echo "Notification hook disabled (NOTIFY_ENABLED=${enabled}). Build result: ${result}."
    }
}

return this
