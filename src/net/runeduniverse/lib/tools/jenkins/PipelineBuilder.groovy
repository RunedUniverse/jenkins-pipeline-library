package net.runeduniverse.lib.tools.jenkins;

class PipelineBuilder implements Serializable {

	def steps

	PipelineBuilder(steps) {
		this.steps = steps;
		echo steps
	}

	def mvn(args) {
		steps.sh "${steps.tool 'Maven'}/bin/mvn -o ${args}"
	}
	
}