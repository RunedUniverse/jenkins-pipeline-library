package net.runeduniverse.lib.tools.jenkins;

class PipelineBuilder implements Serializable {

	def steps

	PipelineBuilder(steps) {
		this.steps = steps;
	}

	def mvn(args) {
		print("print ---")
		//print(steps)
		print("print ---")
		//steps.sh "${steps.tool 'Maven'}/bin/mvn -o ${args}"
	}
	
}