package net.runeduniverse.lib.tools.jenkins;

@Grab('net.runeduniverse.lib.utils:utils-common:1.1.0')
import net.runeduniverse.lib.utils.common.StringUtils;

class MavenProject implements Project {

	private final Object workflow;
	private final Maven mvn;

	private String id = "";
	private String name = "";
	private String path = ".";
	private String modulePath = null;
	private Boolean changed = null;
	private boolean active = true;
	private boolean bom = false;
	private MavenProject parent = null;
	private List<MavenProject> modules = new LinkedList();

	MavenProject(Maven mvn){
		this.mvn = mvn;
		this.workflow = this.mvn.workflow;
	}
	MavenProject(Maven mvn, Map conf){
		this.mvn = mvn;
		this.workflow = this.mvn.workflow;
		this.id = conf.id;
		this.name = conf.name;
		this.path = conf.path;
		this.modulePath = conf.modulePath;
		if(conf.active != null) {
			this.active = conf.active == true;
		}
		if(conf.bom != null) {
			this.bom = conf.bom == true;
		}
	}

	////////////////////////////////////////////////////////////
	// GETTER
	////////////////////////////////////////////////////////////

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getPath() {
		return this.path;
	}

	public String getModulePath() {
		return this.modulePath == null ? this.path : this.modulePath;
	}

	public boolean isParent() {
		return this.parent == null;
	}

	public boolean hasChanged() {
		return this.changed == null ? true : this.changed;
	}

	public boolean isActive() {
		return this.active;
	}

	public boolean isBOM() {
		return this.bom;
	}

	////////////////////////////////////////////////////////////
	// SETTER
	////////////////////////////////////////////////////////////

	public MavenProject setId(String id) {
		this.id = id;
		return this;
	}

	public MavenProject setName(String name) {
		this.name = name;
		return this;
	}
	public MavenProject setPath(String path) {
		this.path = path;
		return this;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public MavenProject setModulePath(String modulePath) {
		this.modulePath = modulePath;
		return this;
	}

	protected void setParent(MavenProject parent) {
		this.parent = parent;
	}

	////////////////////////////////////////////////////////////

	public MavenProject addModule(MavenProject project) {
		project.setParent(this);
		this.modules.add(project);
		return this;
	}

	public MavenProject addModule(Map conf) {
		MavenProject project = new MavenProject(this.mvn, conf);
		this.addModule(project);
		return project;
	}

	public void attachTo(PipelineBuilder builder) {
		builder.attachProject(this);
		this.modules.each {
			it.attachTo(builder);
		}
	}

	public String getVersion(modulePath = null) {
		String modPath = modulePath == null ? "." : modulePath;

		if(this.parent == null) {
			return this.mvn.eval("project.version", this.path, modPath);
		}
		modPath = this.modulePath == null ? this.path : this.modulePath;
		if(modulePath != null) {
			modPath = modPath + '/' + modulePath;
		}
		return this.parent.getVersion(modPath);
	}

	public void purgeCache() {
		if(this.parent != null)
			return;
		this.mvn.purgeCache(this.path);
	}

	public void resolveResources() {
		if(this.parent != null)
			return;
		this.mvn.resolveDependencies(this.path);
	}

	// mark with @NonCPS so Jenkins doesn't try transform it
	// @NonCPS can't execute Pipeline steps!!!
	@NonCPS
	private List<String> toStringList(Object obj){
		List<String> result = new LinkedList();
		if(obj instanceof List) {
			result.addAll(obj.toUnique().grep {
				it instanceof String && !StringUtils.isBlank(it)
			}.collect {
				it.trim()
			});
		} else {
			if(obj instanceof String && !StringUtils.isBlank(obj)) {
				result.add(obj.trim());
			}
		}
		return result;
	}

	@NonCPS
	public List<MavenProject> getModules(Map config = [:]) {
		Closure filter = config.filter instanceof Closure ? config.filter : { p -> true };
		// includeSelf is only applicable to the outermost project
		boolean includeSelf = Boolean.TRUE.equals(config.remove("includeSelf"));
		List<MavenProject> results = new LinkedList();

		if(includeSelf && Boolean.TRUE.equals(filter(this))) {
			results.add(this);
		}

		for (m in this.modules) {
			if(Boolean.TRUE.equals(filter(m))) {
				results.add(m);
			}
			results.addAll(m.getModules(config));
		}
		return results;
	}

	public List<Project> collectProjects(Map config) {
		return getModules(config);
	}

	@NonCPS
	public List<String> getModulePaths(Map config = [:]) {
		Closure filter = config.filter instanceof Closure ? config.filter : { p -> true };
		// includeSelf is only applicable to the outermost project
		boolean includeSelf = Boolean.TRUE.equals(config.remove("includeSelf"));
		String modPath = getModulePath();
		List<String> paths = new LinkedList();

		for (m in this.modules) {
			if(Boolean.TRUE.equals(filter(m))) {
				paths.add(m.getModulePath());
			}
			paths.addAll(m.getModulePaths(config));
		}

		List<String> results = new LinkedList();
		// ensure that the project is mentioned before its modules
		if(includeSelf && Boolean.TRUE.equals(filter(this))) {
			results.add(modPath);
		}
		results.addAll(paths.collect {
			modPath + '/' + it
		});
		return results;
	}

	public String execDev(Map cnf = [:]) {
		List<String> modules = toStringList(cnf.modules);
		if(this.parent == null || Boolean.TRUE.equals(cnf.skipParent)) {
			List<String> profiles = new LinkedList();
			if(!Boolean.TRUE.equals(cnf.skipRepos)) {
				profiles.add(this.workflow.getProperty("REPOS"));
			}
			profiles.addAll(toStringList(cnf.profiles));
			String profilesArg = profiles.isEmpty() ? "" : "-P " + profiles.join(",");
			String goals = toStringList(cnf.goals).join(",");
			String modulesArg = modules.isEmpty() ? "" : "-pl=" + modules.join(",");
			this.workflow.echo("path:       " + this.path);
			return this.mvn.execDev(
					this.path,
					"${profilesArg} ${goals} ${modulesArg}",
					toStringList(cnf.args).join(" ")
					);
		}
		String modPath = getModulePath();
		if(modules.isEmpty()) {
			// select this and all children
			modules.addAll(getModulePaths([ includeSelf: true ]));
		} else {
			modules = modules.collect {
				it.equals(".") ? modPath : modPath + '/' + it
			}
		}
		cnf.modules = modules;
		return this.parent.execDev(cnf);
	}

	public void info(boolean interate = true) {
		this.workflow.echo("id:         " + this.id);
		this.workflow.echo("name:       " + this.name);
		this.workflow.echo("path:       " + this.path);
		if(this.modulePath != null)
			this.workflow.echo("modulePath: " + this.modulePath);
		this.workflow.echo("version:    " + this.path);
		this.workflow.echo("changed:    " + this.changed == null ? "????" : this.changed.toString());

		if(interate) {
			this.modules.each {
				this.workflow.echo("-------------------------");
				it.info();
			}
		}
	}
}