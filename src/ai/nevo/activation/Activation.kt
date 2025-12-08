package ai.nevo.activation

import kotlin.math.*
import kotlin.random.Random

/**
 * An [ActivationFunction] is a function which takes a [Float] value and returns another Float depending on said input.
 * This can be literally any function, ranging from linear function to sinus function.
 */
interface ActivationFunction {
    operator fun invoke(x: Double): Double
}

/**
 * The [Sigmoid] [] [ActivationFunction] can be viewed [here](https://www.geogebra.org/m/QvSuH67g).
 * The [slope] (similar to "a" on linked webpage) can manually be specified to fit different needs.
 */
data class Sigmoid(private val slope: Double = 4.9) : ActivationFunction {
    override fun invoke(x: Double): Double {
        println("x: $x")
        return (1 / (1 + exp(-slope * x)))
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

/**
 * The [Tanh] [] [ActivationFunction] is similar to the [Sigmoid] function, but its output range is (-1, 1) in contrast
 * to Sigmoid's (0, 1) range. The [slope] can be specified when a non-default value is needed.
 */
data class Tanh(private val slope: Double = 2.0) : ActivationFunction {
    override fun invoke(x: Double) = (2 / (1 + exp(-slope * x))) - 1

    companion object {
        private const val serialVersionUID = 0L
    }
}

/**
 * The [Sinus] [] [ActivationFunction] should be self-explanatory. Its output range is `[-1, 1]`.
 * The [compression] can be specified. It should be noted, that values < 1 result in a stretch instead.
 */
data class Sinus(private val compression: Double = 1.0) : ActivationFunction {
    override fun invoke(x: Double) = sin(compression * x)

    companion object {
        private const val serialVersionUID = 0L
    }
}

/**
 * The [Step] [] [ActivationFunction] is a threshold function with an output of either 1 or 0.
 * The [threshold] t (defaults to 0) defines the function in the following way:
 *
 * y = 1 if x >= t else 0
 */
data class Step(private val threshold: Double = 0.0) : ActivationFunction {
    override fun invoke(x: Double) = if (x >= threshold) 1.0 else 0.0

    companion object {
        private const val serialVersionUID = 0L
    }
}

/**
 * The [Sign] [] [ActivationFunction] is a function which returns the sign of the input where the output is either
 * 1, 0 or -1.
 * Function is defined in the following way:
 *
 * y = 1 if x > 0 else -1 if x < 0 else 0
 */
class Sign : ActivationFunction {
    override fun invoke(x: Double) = if (x > 0.0) 1.0 else if (x < -0.0) -1.0 else 0.0
}

/**
 * The [Random] [] [ActivationFunction] returns a random value in the range of [-1, 1) regardless of input. The [random]
 * number generator can be set manually after initialization.
 */
class Random : ActivationFunction {
    override fun invoke(x: Double) = Random.nextDouble() * 2 - 1
}

/**
 * The [Relu] [] [ActivationFunction] returns either its input or 0, whatever is greater.
 */
class Relu : ActivationFunction {
    override fun invoke(x: Double) = if (x > 0.0) x else 0.0
}

/**
 * The [Selu] [] [ActivationFunction] is hard to describe, but you can find a good explanation
 * [here](https://mlfromscratch.com/activation-functions-explained/#selu).
 */
class Selu : ActivationFunction {
    companion object {
        private const val alpha = 1.6732632423543772848170429916717
        private const val lambda = 1.0507009873554804934193349852946
    }

    override fun invoke(x: Double) = lambda * if (x > 0.0) x else alpha * exp(x) - alpha
}

/**
 * The [Silu] [] [ActivationFunction] is similar to the [Relu] function but uses the [Sigmoid] function.
 * Similarly, you can set the "slope" of the Sigmoid part using the [a] value. In this function it controls
 * the amount of leakage for inputs below 0.
 */
data class Silu(private val a: Double = 1.0) : ActivationFunction {
    override fun invoke(x: Double) = (x / (1 + exp(-a * x)))
}

/**
 * The [Linear] [] [ActivationFunction] multiplies the input with the [gradient] (defaults to 1).
 */
data class Linear(private val gradient: Double = 1.0) : ActivationFunction {
    override fun invoke(x: Double) = gradient * x
}

/**
 * This function returns a special [Linear] [] [ActivationFunction], which returns its input as output, called
 * [Identity].
 */
fun Identity() = identity
private val identity by lazy { Linear() }
