import NetworkVisualizerComponent.ConnectionInfo
import NetworkVisualizerComponent.NodeInfo
import NetworkVisualizerComponent.NodeType
import eu.timerertim.knevo.instinct.*
import kotlin.math.*

/**
 * Helper class to extract network structure from a knevo Network instance
 */
object NetworkExtractor {
    // Keep track of previous hidden count to avoid visual jumps
    private var lastHiddenCount = 4

    /**
     * Try to extract the network structure using reflection
     */
    fun tryExtractNetwork(
        network: Network,
        inputs: FloatArray,
        output: FloatArray
    ): NetworkStructure {
        try {
            // Try to examine network structure
            // For now, we'll just use a simulated network since we don't have direct access
            return extractSimulatedNetwork(network, inputs, output)
        } catch (e: Exception) {
            println("Error extracting network: ${e.message}")
            // Return a fallback if extraction fails
            return extractSimulatedNetwork(network, inputs, output)
        }
    }

    /**
     * Extract a simulated network structure based on inputs and outputs
     */
    fun extractSimulatedNetwork(
        network: Network,
        inputs: FloatArray,
        output: FloatArray
    ): NetworkStructure {
        val nodeInfos = mutableListOf<NodeInfo>()
        val connectionInfos = mutableListOf<ConnectionInfo>()
        val nodeValues = mutableMapOf<Int, Float>()

        // Create estimated hidden layer values
        val hiddenNeurons = inferHiddenCount(network)
        val hiddenValues = estimateHiddenValues(inputs, output[0], hiddenNeurons)

        // Input layer
        for (i in inputs.indices) {
            val id = i + 1
            nodeInfos.add(NodeInfo(id, NodeType.INPUT, 50.0, 60.0 + i * 30.0, "Linear"))
            nodeValues[id] = inputs[i]
        }

        // Output layer
        val outputId = inputs.size + hiddenValues.size + 1
        nodeInfos.add(NodeInfo(outputId, NodeType.OUTPUT, 190.0, 90.0, "Sigmoid"))
        nodeValues[outputId] = output[0]

        // Hidden layer
        val hiddenX = 120.0
        if (hiddenValues.isNotEmpty()) {
            val verticalSpacing =
                if (hiddenValues.size > 1) 120.0 / (hiddenValues.size - 1) else 30.0
            val startY = 60.0 + 15.0

            for (i in hiddenValues.indices) {
                val id = inputs.size + i + 1
                val y = startY + i * verticalSpacing
                nodeInfos.add(NodeInfo(id, NodeType.HIDDEN, hiddenX, y, "Tanh"))
                nodeValues[id] = hiddenValues[i]

                // Connect inputs to this hidden node
                for (j in inputs.indices) {
                    val weight = calculateWeightEstimate(inputs[j], hiddenValues[i])
                    connectionInfos.add(ConnectionInfo(j + 1, id, weight))
                }

                // Connect to output
                val outputWeight = calculateWeightEstimate(hiddenValues[i], output[0])
                connectionInfos.add(ConnectionInfo(id, outputId, outputWeight))
            }
        } else {
            // If no hidden layer, connect inputs directly to output
            for (i in inputs.indices) {
                val weight = calculateWeightEstimate(inputs[i], output[0])
                connectionInfos.add(ConnectionInfo(i + 1, outputId, weight))
            }
        }

        return NetworkStructure(nodeInfos, connectionInfos, nodeValues)
    }

    /**
     * Try to infer the number of hidden neurons in the network
     */
    private fun inferHiddenCount(network: Network): Int {
        try {
            // Try to access network structure
            val networkString = network.toString()
            var inferredCount = 0

            // Check if network toString contains information about nodes
            val nodeCountRegex = "nodes=(\\d+)".toRegex()
            val nodeMatch = nodeCountRegex.find(networkString)
            if (nodeMatch != null) {
                val totalNodes = nodeMatch.groupValues[1].toIntOrNull() ?: 0
                // Assume input = 3, output = 1, rest are hidden
                inferredCount = (totalNodes - 4).coerceAtLeast(0)
            } else {
                // Base estimate on network generation or hash code
                val generationRegex = "generation=(\\d+)".toRegex()
                val generationMatch = generationRegex.find(networkString)

                inferredCount = if (generationMatch != null) {
                    val generation = generationMatch.groupValues[1].toIntOrNull() ?: 0
                    // Start with 1 neuron, add 1 every 5 generations
                    1 + (generation / 5).coerceAtMost(10) // Cap at reasonable maximum
                } else {
                    // Use hashCode for some randomness but keep it stable
                    val hash = abs(network.hashCode() % 5) + 1 // 1-5 range
                    hash
                }
            }

            // To prevent dramatic changes, only change by at most 1 at a time
            if (abs(inferredCount - lastHiddenCount) > 1) {
                inferredCount = if (inferredCount > lastHiddenCount)
                    lastHiddenCount + 1
                else
                    lastHiddenCount - 1
            }

            lastHiddenCount = inferredCount
            return inferredCount
        } catch (e: Exception) {
            // If anything fails, return the last count for stability
            return lastHiddenCount
        }
    }

    /**
     * Estimate hidden layer values from inputs and outputs
     */
    private fun estimateHiddenValues(
        inputs: FloatArray,
        output: Float,
        hiddenCount: Int
    ): FloatArray {
        if (hiddenCount <= 0) return FloatArray(0)

        val hiddenValues = FloatArray(hiddenCount)

        // Create meaningful specializations for up to 8 hidden neurons
        if (hiddenCount <= 8) {
            when (hiddenCount) {
                1 -> {
                    // Simple activation based on all inputs
                    hiddenValues[0] = (inputs[0] * 0.5f + inputs[1] * 0.3f - inputs[2] * 0.7f) *
                        if (output > 0.5f) 1.2f else 0.3f
                }

                2 -> {
                    // Two specializations: vertical position and velocity
                    hiddenValues[0] = inputs[0] * 1.5f  // Bird Y position detector
                    hiddenValues[1] = -inputs[2] * 1.3f  // Velocity monitor
                }

                3 -> {
                    // Three specializations covering main inputs
                    hiddenValues[0] = inputs[0] * 1.5f  // Bird Y position detector
                    hiddenValues[1] = (1.0f - abs(inputs[1])) * 1.2f  // Distance analyzer
                    hiddenValues[2] = -inputs[2] * 1.3f  // Velocity monitor
                }

                4 -> {
                    // Four specializations with decision neuron
                    hiddenValues[0] = inputs[0] * 1.5f  // Bird Y position detector
                    hiddenValues[1] = (1.0f - abs(inputs[1])) * 1.2f  // Distance analyzer
                    hiddenValues[2] = -inputs[2] * 1.3f  // Velocity monitor
                    hiddenValues[3] = (inputs[0] * 0.6f + inputs[1] * 0.2f - inputs[2] * 0.8f) *
                        if (output > 0.5f) 1.5f else -0.5f  // Decision integrator
                }

                else -> {
                    // For 5+ neurons, add some neurons that combine multiple features
                    hiddenValues[0] = inputs[0] * 1.5f  // Bird Y
                    hiddenValues[1] = (1.0f - abs(inputs[1])) * 1.2f  // Distance
                    hiddenValues[2] = -inputs[2] * 1.3f  // Velocity
                    hiddenValues[3] = (inputs[0] * 0.6f + inputs[1] * 0.2f - inputs[2] * 0.8f) *
                        if (output > 0.5f) 1.5f else -0.5f  // Decision

                    // Add additional specialized neurons
                    hiddenValues[4] = (inputs[0] + 0.5f) * 1.1f  // High position detector

                    if (hiddenCount >= 6) {
                        hiddenValues[5] = inputs[1] * 2.0f  // Distance amplifier
                    }

                    if (hiddenCount >= 7) {
                        hiddenValues[6] =
                            (inputs[0] * inputs[2]) * 1.4f  // Position-velocity correlation
                    }

                    if (hiddenCount >= 8) {
                        hiddenValues[7] = if (output > 0.5f) 0.9f else -0.9f  // Decision memory
                    }
                }
            }
        } else {
            // For very large networks, just fill with some patterns
            for (i in 0 until hiddenCount) {
                val progress = i.toFloat() / hiddenCount.toFloat()
                val value = sin(progress * 6).toFloat() * 0.8f
                hiddenValues[i] = value
            }

            // Make sure at least some values correlate with output
            for (i in 0 until minOf(4, hiddenCount)) {
                hiddenValues[i] = if (output > 0.5f) 0.8f else -0.5f
            }
        }

        // Apply tanh-like activation to all values
        for (i in hiddenValues.indices) {
            hiddenValues[i] =
                (hiddenValues[i] / (1.0f + abs(hiddenValues[i])) * 2.0f).coerceIn(-1.0f, 1.0f)
        }

        return hiddenValues
    }

    /**
     * Calculate a weight estimate between two neuron values
     */
    private fun calculateWeightEstimate(fromValue: Float, toValue: Float): Float {
        // If the signs match, use a positive weight, otherwise negative
        val sign = if ((fromValue >= 0 && toValue >= 0) || (fromValue < 0 && toValue < 0)) 1 else -1

        // Weight magnitude is based on the correlation between values
        val magnitude = (abs(fromValue) * abs(toValue) * 1.5f).coerceAtMost(1.0f)

        return sign * magnitude
    }

    /**
     * Class representing a neural network's structure
     */
    data class NetworkStructure(
        val nodes: List<NodeInfo>,
        val connections: List<ConnectionInfo>,
        val nodeValues: Map<Int, Float>
    )
}
