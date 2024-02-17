package net.runeduniverse.lib.tools.jenkins;

class PipelineBuilder implements Serializable {

	def steps

	PipelineBuilder(steps) {
		this.steps = steps;
		print(steps)
	}

	def mvn(args) {
		print(steps)
		//steps.sh "${steps.tool 'Maven'}/bin/mvn -o ${args}"
	}
	
}