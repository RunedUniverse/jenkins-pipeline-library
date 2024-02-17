package net.runeduniverse.lib.tools.jenkins;

class PipelineBuilder implements Serializable {

	def workflow;
	private VersionSystem vsys = null;
	private Map<String,Project> projects = new LinkedHashMap();

	PipelineBuilder(workflow) {
		this.workflow = workflow;
		this.workflow.setProperty("GLOBAL_MAVEN_SETTINGS", "/srv/jenkins/.m2/global-settings.xml")
	}

	public void setVersionSystem(VersionSystem vsys) {
		this.vsys = vsys;
		this.vsys.setWorkflow(this.workflow);
	}

	public void attachProject(Project project) {
		this.projects.put(project.getId(), project);
	}

	public void logProjects() {
		this.projects.each { it.info(false); }
	}

	public void checkChanges() {
		this.projects.each { it.setChanged(!this.vsys.versionTagExists(it.getId(), id.getVersion())) }
	}

	def mvn(args) {
		this.workflow.sh "echo Method 1: ${this.workflow.GLOBAL_MAVEN_SETTINGS}"
		//steps.sh "${steps.tool 'Maven'}/bin/mvn -o ${args}"
	}
}