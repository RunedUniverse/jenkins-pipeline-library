def call(Map cnf = [:]) {
	stage("${cnf.name}"){
		when {
			environment name: "${cnf.envTrigger}", value: '1'
		}
		steps {
			mvnDev(cnf)
		}
	}
}

