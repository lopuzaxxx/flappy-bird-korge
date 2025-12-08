package ai.nevo.instinct

import ai.nevo.activation.*

/**
 * The global [InstinctInstance] which is used when no other InstinctInstance is specified.
 * For example:
 *
 *      val n = InstinctNetwork()
 *      val n = globalInstinctInstance.Network()
 *
 *  does the same.
 */
var globalInstinctInstance = InstinctInstance(2, 1)

/**
 * The main class when interacting with the
 * [Instinct](https://towardsdatascience.com/neuro-evolution-on-steroids-82bd14ddc2f6) implementation in Knevo.
 *
 * It contains every necessary configuration information needed for basic usage of this neuroevolution algorithm.
 *
 * The algorithm features:
 * - Crossover
 * - Mutation
 * - Complexification
 * - Simplification
 * - Recurrent Connections
 * - Gated Connections
 * - Mutable [ActivationFunction]s
 * - Different [SelectionFunction]s
 * - Training Penalties (regarding growth/size of network)
 *
 * Configurable parameters:
 * - [inputs]: The amount of input neurons
 * - [outputs]: The amount of output neurons
 * - [inputActivations]: A list of possible ActivationFunctions for input neurons
 * - [outputActivations]: A list of possible ActivationFunctions for output neurons
 * - [hiddenActivations]: A list of possible ActivationFunctions for hidden neurons
 * - [mutateAddNodeChance]: The chance of adding a new hidden neuron
 * - [mutateAddForwardConnectionChance]: The chance of adding a forward connection
 * - [mutateAddSelfConnectionChance]: The chance of adding a connection from and to the same neuron
 * - [mutateAddRecurrentConnectionChance]: The chance of adding a recurrent connection
 * - [mutateAddGateChance]: The chance of adding a gate neuron to an existing connection
 * - [mutateWeightChance]: The chance of mutating the weight of an existing connection
 * - [mutateBiasChance]: The chance of mutating the bias of an existing neuron
 * - [mutateActivationChance]: The chance of changing the ActivationFunction of a neuron
 * - [mutateRemoveNodeChance]: The chance of removing an existing hidden neuron
 * - [mutateRemoveConnectionChance]: The chance of removing an existing connection
 * - [mutateRemoveGateChance]: The chance of removing the gate neuron from an existing connection with gate neuron
 *
 * Every mutation chance consists of the number before the decimal point and one after the decimal point. The first
 * number increased by one equals the amount of mutation tries. The one after the decimal point determines the chance of
 * the mutation actually occurring per try.
 *
 *      mutateAddNodeChance = 1.05F // 2 tries, 5% chance each
 *      mutateRemoveConnectionChance = 0.10F // 1 try, 10% chance
 *      mutateAddRecurrentConnectionChance = 0.0F // No try, 0% chance
 */
data class InstinctInstance(
    val inputs: Int,
    val outputs: Int,

    val inputActivations: List<ActivationFunction> = listOf(Identity()),
    val outputActivations: List<ActivationFunction> = listOf(Identity()),
    val hiddenActivations: List<ActivationFunction> =
        listOf(Sigmoid(), Tanh(), Step(), Sign(), Linear(), Sinus(), Relu(), Selu(), Silu()),

    val mutateAddNodeChance: Float = 1.05F,
    val mutateAddForwardConnectionChance: Float = 5.20F,
    val mutateAddSelfConnectionChance: Float = 1.025F,
    val mutateAddRecurrentConnectionChance: Float = 2.01F,
    val mutateAddGateChance: Float = 2.15F,
    val mutateWeightChance: Float = 5.75F,
    val mutateBiasChance: Float = 2.50F,
    val mutateActivationChance: Float = 0.01F,
    val mutateRemoveNodeChance: Float = 0.025F,
    val mutateRemoveConnectionChance: Float = 0.10F,
    val mutateRemoveGateChance: Float = 1.10F
)
