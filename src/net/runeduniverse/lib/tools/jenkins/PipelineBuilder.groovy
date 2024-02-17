package net.runeduniverse.lib.tools.jenkins;

class PipelineBuilder implements Serializable {

	def workflow;
	private VersionSystem vsys = null;
	private List<BuildTool> buildTools = new LinkedList();
	private Map<String,Project> projects = new LinkedHashMap();

	PipelineBuilder(workflow) {
		this.workflow = workflow;
		this.workflow.setProperty("GLOBAL_MAVEN_SETTINGS", "/srv/jenkins/.m2/global-settings.xml")
	}

	public void setVersionSystem(VersionSystem vsys) {
		this.vsys = vsys;
		this.vsys.setWorkflow(this.workflow);
	}
	
	public void addBuildTool(BuildTool tool) {
		this.buildTools.add(tool);
	}

	public void attachProject(Project project) {
		this.projects.put(project.getId(), project);
	}

	public void logProjects() {
		this.projects.each { it.info(false); }
	}

	public void checkChanges() {
		this.projects.each { it.value.setChanged(!this.vsys.versionTagExists(it.value.getId(), it.value.getVersion())) }
	}
	
	public void purgeBuildCaches() {
		this.buildTools.each { it.purgeCache(); }
	}
	
	public void resolveResources() {
		this.projects.each { it.value.resolveResources(); }
	}
	
	public boolean hasChangedProjects() {
		boolean changed = false;
		this.projects.each { if( changed && it.value.hasChanged()) changed = true; }
		return changed;
	}
}