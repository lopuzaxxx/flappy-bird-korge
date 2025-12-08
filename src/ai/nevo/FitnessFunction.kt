package ai.nevo

/**
 * [FitnessFunction]s determine the [fitness][Genome.fitness] of a given [Genome]. It can be used like an [Environment]
 * for training.
 */
interface FitnessFunction<in G : Genome> : Environment<G>, suspend (G) -> Double {
    override suspend fun evaluateFitness(population: List<G>) {
        population.forEach {
            it.fitness = this(it)
        }
    }

    companion object {
        /**
         * Creates a [FitnessFunction] from the given [function].
         */
        operator fun <G : Genome> invoke(function: suspend (G) -> Double): FitnessFunction<G> =
            object : FitnessFunction<G>, suspend (G) -> Double by function {}
    }
}
