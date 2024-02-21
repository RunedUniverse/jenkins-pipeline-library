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

	@NonCPS
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
					project.path,
					"${profilesArg} ${goals} ${modulesArg}",
					toStringList(cnf.args).join(" ")
					);
		}
		String modPath = project.getModulePath();
		if(modules.isEmpty()) {
			// select this and all children
			//modules.addAll(getModulePaths([ includeSelf: true ]));
		} else {
			modules = modules.collect {
				it.equals(".") ? modPath : modPath + '/' + it
			}
		}
		cnf.modules = modules;
		return mvnExecDev(project.parent, cnf);
	}

	@NonCPS
	public static String mvnEval(MavenProject project, String expression, String modulePath) {
		final Maven mvn = project.mvn;

		while (project.parent != null) {
			// save module-path in case of null or .
			String modPath = project.getModulePath();
			project = project.parent;
			if(modulePath == null || ".".equals(modulePath)) {
				modulePath = modPath;
			}else {
				modulePath = modPath + '/' + modulePath;
			}
		}
		return mvn.eval(expression, project.path, modulePath == null ? "." : modulePath);
	}
}