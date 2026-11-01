// Post Actions: release executors left behind by aborted builds.
//
// A build that is killed while waiting for a node leaves an
// ExecutorStepExecution.PlaceholderTask holding the executor.  The
// placeholder lives on the controller, so restarting an agent does not
// clear it.  At the end of a pipeline every branch of the current build
// has already finished, therefore any placeholder still present is stale
// and can be interrupted safely.

import com.cloudbees.groovy.cps.NonCPS
import jenkins.model.Jenkins

@NonCPS
def interruptStalePlaceholders() {
    def interrupted = []
    Jenkins.get().computers.each { computer ->
        computer.executors.each { executor ->
            hudson.model.Executor ex = (hudson.model.Executor) executor
            if (ex.isBusy()) {
                def executable = ex.getCurrentExecutable()
                def className = executable?.getClass()?.getName() ?: ''
                if (className.contains('PlaceholderTask')) {
                    interrupted << "${computer.getDisplayName()}/${ex.getNumber()}: ${executable}"
                    ex.interrupt()
                }
            }
        }
    }
    return interrupted
}

def cleanup() {
    def interrupted = interruptStalePlaceholders()
    if (interrupted.isEmpty()) {
        echo 'Post Actions: no stale executors.'
    } else {
        echo "Post Actions: interrupted ${interrupted.size()} stale executor(s):\n" + interrupted.join('\n')
    }
}

return this
