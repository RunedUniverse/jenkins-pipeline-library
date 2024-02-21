package net.runeduniverse.lib.tools.jenkins;

@Grab('net.runeduniverse.lib.utils:utils-common:1.1.0')
import net.runeduniverse.lib.utils.common.StringUtils;

class PUtils {

	// mark with @NonCPS so Jenkins doesn't try transform it
	// @NonCPS can't execute Pipeline steps!!!
	@NonCPS
	public static List<String> toStringList(Object obj){
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

	public static mvnExecDev(MavenProject project, Map<String,Object> cnf = [:]) {
		final Maven mvn = project.mvn;
		List<String> modules = toStringList(cnf.modules);
		if(project.parent == null || Boolean.TRUE.equals(cnf.skipParent)) {
			List<String> profiles = new LinkedList();
			if(!Boolean.TRUE.equals(cnf.skipRepos)) {
				profiles.addAll(mvn.repoProfiles);
			}
			profiles.addAll(toStringList(cnf.profiles));
			String profilesArg = profiles.isEmpty() ? "" : "-P " + profiles.join(",");
			String goals = toStringList(cnf.goals).join(",");
			String modulesArg = modules.isEmpty() ? "" : "-pl=" + modules.join(",");
			return mvn.execDev(
					project.getPath(),
					"${profilesArg} ${goals} ${modulesArg}",
					toStringList(cnf.args).join(" ")
					);
		}
		String modPath = project.getModulePath();
		if(modules.isEmpty()) {
			// select this and all children
			modules.addAll(collectMvnModulePaths(project, true));
		} else {
			modules = modules.collect {
				it.equals(".") ? modPath : modPath + '/' + it
			}
		}
		cnf.modules = modules;
		return mvnExecDev(project.getParent(), cnf);
	}

	@NonCPS
	public static String mvnEval(MavenProject project, String expression, String modulePath = null) {
		final Maven mvn = project.mvn;

		while (project.getParent() != null) {
			// save module-path in case of null or .
			String modPath = project.getModulePath();
			project = project.getParent();
			if(modulePath == null || ".".equals(modulePath)) {
				modulePath = modPath;
			}else {
				modulePath = modPath + '/' + modulePath;
			}
		}
		return mvn.eval(expression, project.getPath(), modulePath == null ? "." : modulePath);
	}

	// you must not use recursion -> it errors out!
	@NonCPS
	public static List<MavenProject> collectMvnModules(MavenProject project, boolean includeSelf) {
		final List<MavenProject> results = new LinkedList();
		final List<MavenProject> searchList = new LinkedList();
		final List<MavenProject> moduleList = new LinkedList();

		if(includeSelf) {
			results.add(project);
		}

		searchList.addAll(project.modules);

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

	@NonCPS
	public static List<MavenProject> collectMvnModules(MavenProject project, Map<String,Object> config = [:]) {
		final Closure filter = config.filter instanceof Closure ? config.filter : { p -> true };
		return collectMvnModules(
				project,
				Boolean.TRUE.equals(config.includeSelf)
				).findAll {
					Boolean.TRUE.equals(filter(it))
				};
	}

	@NonCPS
	public static Map<MavenProject, String> collectMvnModulePaths(MavenProject project, boolean includeSelf) {
		final Map<MavenProject, String> results = new LinkedHashMap();
		final List<MavenProject> searchList = new LinkedList();
		final List<MavenProject> moduleList = new LinkedList();

		if(includeSelf) {
			results.put(project, ".");
		}

		searchList.addAll(project.modules);

		while (!searchList.isEmpty()) {
			for (entry in searchList) {
				results.put(entry, (this == entry.getParent() ? "" : results.get(entry.getParent()) + "/") + entry.getModulePath())
				moduleList.addAll(entry.modules);
			}
			searchList.clear();
			searchList.addAll(moduleList);
		}

		return results;
	}

	@NonCPS
	public static List<String> collectMvnModulePaths(MavenProject project, Map<String,Object> config = [:]) {
		final Closure filter = config.filter instanceof Closure ? config.filter : { p -> true };
		return collectMvnModulePaths(
				project,
				Boolean.TRUE.equals(config.includeSelf)
				).findAll {
					Boolean.TRUE.equals(filter(it.key))
				}.collect {
					it.value
				};
	}
}