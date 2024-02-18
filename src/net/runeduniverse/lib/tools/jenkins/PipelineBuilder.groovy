package net.runeduniverse.lib.tools.jenkins;

import org.jenkinsci.plugins.pipeline.modeldefinition.Utils;

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
		this.projects.each { it.value.info(false); }
	}

	public void checkChanges() {
		this.projects.each { it.value.setChanged(!this.vsys.versionTagExists(it.value.getId(), it.value.getVersion())) }
	}

	public void purgeBuildCaches() {
		this.projects.each { it.value.purgeCache(); }
	}

	public void resolveResources() {
		this.projects.each { it.value.resolveResources(); }
	}

	public List<Project> collectProjects(){
		return this.projects.collect { it.value };
	}

	public Map<String,Closure> forEachProject(Map<String,Closure> config = [filter: { p -> true }, when : { p -> true }, name : { p -> p.getName() }], Closure block){
		Closure filter = config.filter instanceof Closure ? config.filter : { p -> true };
		return this.projects.collect {
			it.value
		}.findAll {
			Boolean.TRUE.equals(filter(it));
		}.collectEntries { project ->
			String nameTxt = config.name instanceof Closure ? config.name(project) : project.getName();
			if(nameTxt == null) {
				nameTxt = project.getId();
			}
			Boolean whenValue = config.when instanceof Closure ? config.when(project) : true;
			if(whenValue == null) {
				whenValue = false;
			}

			return [
				(nameTxt): {
					this.workflow.stage(nameTxt) {
						block.resolveStrategy = Closure.DELEGATE_ONLY;
						block.delegate = [post : owner.post];
						if (whenValue) {
							block(project);
						} else {
							Utils.markStageSkippedForConditional(nameTxt);
						}
					}
				}
			];
		}
	}

	public boolean hasChangedProjects() {
		boolean changed = false;
		this.projects.each { if( changed || it.value.hasChanged()) changed = true; }
		return changed;
	}
}