package net.runeduniverse.lib.tools.jenkins;

class PipelineBuilder implements Serializable {

	def workflow;
	def parentProject;

	PipelineBuilder(workflow) {
		this.workflow = workflow;
		this.workflow.setProperty("GLOBAL_MAVEN_SETTINGS", "/srv/jenkins/.m2/global-settings.xml")
	}

	def mvn(args) {
		this.workflow.sh "echo Method 1: ${this.workflow.GLOBAL_MAVEN_SETTINGS}"
		//steps.sh "${steps.tool 'Maven'}/bin/mvn -o ${args}"
	}
	
}