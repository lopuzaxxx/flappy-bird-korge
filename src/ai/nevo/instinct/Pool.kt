package ai.nevo.instinct

import ai.nevo.*
import ai.nevo.instinct.InstinctNetwork.Companion.offspring
import ai.nevo.selection.*
import korlibs.io.async.*
import kotlinx.coroutines.*
import kotlin.random.*

typealias Pool = InstinctPool

/**
 * A pretty generic, simple [Population] implementation used with the algorithm described in [InstinctInstance]. It does
 * not feature speciation, but multithreading, different [SelectionFunction]s and size penalties for the
 * [InstinctNetwork]s. The [instance][InstinctPool.instance] is used for all operations requiring an InstinctInstance.
 *
 * Configurable Parameters:
 * - [populationSize]: The amount of Networks in this Population
 * - [batchSize]: The amount of Networks in a single batch. Batches are evaluated and breded in parallel.
 * - [elitism]: This many best performing networks will be in the next generation without any changes
 * - [crossoverChance]: The chance of performing a crossover when breeding the next generation (otherwise mutate)
 * - [nodesGrowth]: The value subtracted from the [fitness][InstinctNetwork.fitness] of a network per hidden neuron
 * - [connectionsGrowth]: The value subtracted from the fitness of a network per connection
 * - [gatesGrowth]: The value subtracted from the fitness of a network per gated connection
 * - [select]: The SelectionFunction used by default when [breedNewGeneration][InstinctPool.breedNewGeneration] is
 * invoked
 */
fun InstinctInstance.Pool(
    populationSize: Int? = null,
    batchSize: Int? = null,
    elitism: Int? = null,
    crossoverChance: Float? = null,
    nodesGrowth: Float? = null,
    connectionsGrowth: Float? = null,
    gatesGrowth: Float? = null,
    select: SelectionFunction? = null
) = InstinctPool(
    populationSize = populationSize ?: 400,
    batchSize = batchSize ?: populationSize ?: 400,
    elitism = elitism ?: 5,
    crossoverChance = crossoverChance ?: 0.75f,
    nodesGrowth = nodesGrowth ?: 0f,
    connectionsGrowth = connectionsGrowth ?: 0f,
    gatesGrowth = gatesGrowth ?: 0f,
    select = select ?: Power()
)

/**
 * A pretty generic, simple [Population] implementation used with the algorithm described in [InstinctInstance]. It does
 * not feature speciation, but multithreading, different [SelectionFunction]s and size penalties for the
 * [InstinctNetwork]s. The [instance][InstinctPool.instance] is used for all operations requiring an InstinctInstance.
 *
 * Configurable Parameters:
 * - [populationSize][InstinctPool.size]: The amount of Networks in this Population
 * - [batchSize]: The amount of Networks in a single batch. Batches are evaluated and breded in parallel.
 * - [elitism]: This many best performing networks will be in the next generation without any changes
 * - [crossoverChance]: The chance of performing a crossover when breeding the next generation (otherwise mutate)
 * - [nodesGrowth]: The value subtracted from the [fitness][InstinctNetwork.fitness] of a network per hidden neuron
 * - [connectionsGrowth]: The value subtracted from the fitness of a network per connection
 * - [gatesGrowth]: The value subtracted from the fitness of a network per gated connection
 * - [select]: The SelectionFunction used by default when [breedNewGeneration][InstinctPool.breedNewGeneration] is
 * invoked
 */
class InstinctPool private constructor(
    val batchSize: Int,
    val elitism: Int,
    val crossoverChance: Float,
    val nodesGrowth: Float,
    val connectionsGrowth: Float,
    val gatesGrowth: Float,
    val select: SelectionFunction,
    val instance: InstinctInstance,
    private val pool: MutableList<InstinctNetwork>
) : Population<InstinctNetwork>, List<InstinctNetwork> by pool {

    /**
     * A pretty generic, simple [Population] implementation used with the algorithm described in [InstinctInstance]. It
     * does not feature speciation, but multithreading, different [SelectionFunction]s and size penalties for the
     * [InstinctNetwork]s. The [instance][InstinctPool.instance] is used for all operations requiring an
     * InstinctInstance.
     *
     * Configurable Parameters:
     * - [populationSize]: The amount of Networks in this Population
     * - [batchSize]: The amount of Networks in a single batch. Batches are evaluated and breded in parallel.
     * - [elitism]: This many best performing networks will be in the next generation without any changes
     * - [crossoverChance]: The chance of performing a crossover when breeding the next generation (otherwise mutate)
     * - [nodesGrowth]: The value subtracted from the [fitness][InstinctNetwork.fitness] of a network per hidden neuron
     * - [connectionsGrowth]: The value subtracted from the fitness of a network per connection
     * - [gatesGrowth]: The value subtracted from the fitness of a network per gated connection
     * - [select]: The SelectionFunction used by default when [breedNewGeneration][InstinctPool.breedNewGeneration] is
     * invoked
     */
    constructor(
        populationSize: Int = 400,
        batchSize: Int = populationSize,
        elitism: Int = 5,
        crossoverChance: Float = 0.75F,
        nodesGrowth: Float = 0F,
        connectionsGrowth: Float = 0F,
        gatesGrowth: Float = 0F,
        select: SelectionFunction = Power(),
        instance: InstinctInstance = globalInstinctInstance
    ) : this(
        batchSize,
        elitism,
        crossoverChance,
        nodesGrowth,
        connectionsGrowth,
        gatesGrowth,
        select,
        instance,
        Array(populationSize) { instance.Network() }.toMutableList()
    )

    /**
     * Keeps track of the current [generation] in this [Pool]. Is increased automatically upon invoking
     * [breedNewGeneration].
     */
    override var generation = 0L
        private set

    /**
     * Evolves this [InstinctPool] using a given [environment] and [select] method. It invokes [evaluateFitness] and
     * then [breedNewGeneration].
     */
    fun evolve(environment: Environment<InstinctNetwork>, select: SelectionFunction) {
        evaluateFitness(environment)
        breedNewGeneration(select)
    }

    override fun evaluateFitness(environment: Environment<InstinctNetwork>) {
        runBlockingNoJs(Dispatchers.Default) {
            pool.chunked(batchSize)
                .map { batch ->
                    launch {
                        environment.evaluateFitness(batch)
                        batch.forEach {
                            it.score -= (it.nodes.size - instance.inputs - instance.outputs) * nodesGrowth +
                                it.connections.size * connectionsGrowth +
                                it.connections.count { con -> con.gater != null } * gatesGrowth
                        }
                    }
                }.joinAll()
        }
    }

    /**
     * Breeds a new generation based on the [evaluated Fitness][evaluateFitness]. It uses
     * the configured [select] method when selecting [InstinctNetwork]s during breeding. It increases [generation].
     */
    override fun breedNewGeneration() = breedNewGeneration(select)

    /**
     * Breeds a new generation based on the [evaluated Fitness][evaluateFitness]. It uses
     * the given [select] method when selecting [InstinctNetwork]s during breeding. It increases [generation].
     */
    fun breedNewGeneration(select: SelectionFunction) {
        pool.sortDescending()
        if (select is FitnessProportionate) select.reset()

        val newPopulation = runBlockingNoJs(Dispatchers.Default) {
            pool.chunked(batchSize)
                .map { batch ->
                    async(Dispatchers.Default) {
                        val size = batch.size
                        Array(size) {
                            produceNextMember()
                        }.asList()
                    }
                }.flatMap { it.await() }
        }

        for (index in elitism until size) {
            pool[index] = newPopulation[index]
        }

        generation++
    }

    private fun produceNextMember(): InstinctNetwork {
        return if (Random.nextFloat() < crossoverChance) {
            // Offspring
            val parent1 = select(this)
            val parent2 = select(filter { it != parent1 })

            offspring(parent1, parent2)
        } else {
            // Mutated Member
            Network(select(this)).apply {
                mutate()
            }
        }
    }

}
