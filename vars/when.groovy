// see https://comquent.de/en/skipped-stages-in-jenkins-scripted-pipeline/
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils;

def call(boolean condition, body) {
	def config = [:];
	body.resolveStrategy = Closure.OWNER_FIRST;
	body.delegate = config;

	if (condition) {
		body()
	} else {
		Utils.markStageSkippedForConditional(STAGE_NAME)
	}
}

