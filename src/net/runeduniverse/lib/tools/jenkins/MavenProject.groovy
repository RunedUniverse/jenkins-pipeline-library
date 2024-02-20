package net.runeduniverse.lib.tools.jenkins;

@Grab('net.runeduniverse.lib.utils:utils-common:1.1.0')
import net.runeduniverse.lib.utils.common.StringUtils;

class MavenProject implements Project {

	private final Set<MavenProject> modules = new LinkedHashSet();

	private final Object workflow;
	private final Maven mvn;

	private String id = "";
	private String name = "";
	private String path = ".";
	private String modulePath = null;
	private String packagingProcedure = null;
	private String version = null;
	private Boolean changed = null;
	private boolean active = true;
	private boolean bom = false;
	private MavenProject parent = null;

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

	@NonCPS
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

		for (m in this.modules) {
			m.attachTo(builder);
		}
	}

	@NonCPS
	public String eval(String expression, String modulePath = null) {
		if(this.parent == null) {
			return this.mvn.eval(expression, this.path, modulePath == null ? "." : modulePath);
		}
		String modPath = getModulePath();
		if(modulePath != null && !".".equals(modulePath)) {
			modPath = modPath + '/' + modulePath;
		}
		return this.parent.getVersion(modPath);
	}

	@NonCPS
	public String getVersion() {
		if(this.version == null) {
			this.version = getVersion(".");
		}
		return this.version;
	}

	@NonCPS
	public String getVersion(String modulePath) {
		return eval("project.version", modulePath);
	}

	@NonCPS
	public String getPackagingProcedure() {
		if(this.packagingProcedure == null) {
			this.packagingProcedure = getPackagingProcedure(".");
		}
		return this.packagingProcedure;
	}

	@NonCPS
	public String getPackagingProcedure(String modulePath) {
		return eval("project.packaging", modulePath);
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

	// you must not use recursion -> it errors out!
	@NonCPS
	protected List<MavenProject> _getModules(boolean includeSelf) {
		List<MavenProject> results = new LinkedList();
		List<MavenProject> searchList = new LinkedList();
		List<MavenProject> moduleList = new LinkedList();

		if(includeSelf) {
			results.add(this);
		}

		searchList.addAll(this.modules);

		while (!searchList.isEmpty()) {
			for (entry in searchList) {
				results.add(entry);
				moduleList.addAll(entry.modules);
			}
			searchList.clear();
			searchList.addAll(moduleList);
		}

		return results.toUnique {
			it.hashCode()
		}
	}

	public List<MavenProject> getModules(Map config = [:]) {
		Closure filter = config.filter instanceof Closure ? config.filter : { p -> true };
		return _getModules(
				Boolean.TRUE.equals(config.includeSelf)
				).findAll {
					Boolean.TRUE.equals(filter(it))
				};
	}

	public List<Project> collectProjects(Map config) {
		return getModules(config);
	}

	@NonCPS
	protected Map<MavenProject, String> _getModulePaths(boolean includeSelf) {
		Map<MavenProject, String> results = new LinkedHashMap();
		List<MavenProject> searchList = new LinkedList();
		List<MavenProject> moduleList = new LinkedList();

		if(includeSelf) {
			results.put(this, ".");
		}

		searchList.addAll(this.modules);

		while (!searchList.isEmpty()) {
			for (entry in searchList) {
				results.put(entry, (this == entry.parent ? "" : results.get(entry.parent) + "/") + entry.getModulePath())
				moduleList.addAll(entry.modules);
			}
			searchList.clear();
			searchList.addAll(moduleList);
		}

		return results;
	}

	public List<String> getModulePaths(Map config = [:]) {
		Closure filter = config.filter instanceof Closure ? config.filter : { p -> true };
		return _getModulePaths(
				Boolean.TRUE.equals(config.includeSelf)
				).findAll {
					Boolean.TRUE.equals(filter(it.key))
				}.collect {
					it.value
				};
	}

	public String execDev(Map cnf = [:]) {
		List<String> modulesList = toStringList(cnf.modules);
		if(this.parent == null || Boolean.TRUE.equals(cnf.skipParent)) {
			List<String> profiles = new LinkedList();
			if(!Boolean.TRUE.equals(cnf.skipRepos)) {
				profiles.add(this.workflow.getProperty("REPOS"));
			}
			profiles.addAll(toStringList(cnf.profiles));
			String profilesArg = profiles.isEmpty() ? "" : "-P " + profiles.join(",");
			String goals = toStringList(cnf.goals).join(",");
			String modulesArg = modulesList.isEmpty() ? "" : "-pl=" + modulesList.join(",");
			this.workflow.echo("path:       " + this.path);
			return this.mvn.execDev(
					this.path,
					"${profilesArg} ${goals} ${modulesArg}",
					toStringList(cnf.args).join(" ")
					);
		}
		String modPath = getModulePath();
		if(modulesList.isEmpty()) {
			// select this and all children
			modulesList.addAll(getModulePaths([ includeSelf: true ]));
		} else {
			modulesList = modulesList.collect {
				it.equals(".") ? modPath : modPath + '/' + it
			}
		}
		cnf.modulesList = modulesList;
		return this.parent.execDev(cnf);
	}

	public void info(boolean interate = true) {
		this.workflow.echo("id:         " + this.id);
		this.workflow.echo("name:       " + this.name);
		this.workflow.echo("path:       " + this.path);
		if(this.modulePath != null)
			this.workflow.echo("modulePath: " + this.modulePath);
		this.workflow.echo("version:    " + this.getVersion());
		this.workflow.echo("changed:    " + this.changed == null ? "????" : this.changed.toString());

		if(interate) {
			for (m in this.modules) {
				this.workflow.echo("-------------------------");
				m.info();
			}
		}
	}
}