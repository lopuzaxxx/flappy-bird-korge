import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.*
import korlibs.korge.view.align.centerXOn
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Component that visualizes a neural network with dynamic NEAT topology
 */
class NetworkVisualizerComponent : Container() {
    private val nodeRadius = 10.0
    private val baseWidth = 240.0
    private val baseHeight = 220.0

    // Layer positions
    private val inputX = 50.0
    private val outputX = 190.0
    private val startY = 60.0

    // Node tracking
    private val nodes = mutableMapOf<Int, Circle>() // Map node IDs to circles
    private val connections = mutableListOf<Pair<Graphics, ConnectionInfo>>()
    private val valueLabels = mutableMapOf<Int, Text>()
    private val nodeDescriptions = mutableMapOf<Int, Text>()

    // Background and container elements
    private lateinit var background: Graphics
    private lateinit var infoTexts: Container
    private lateinit var nodeCountText: Text

    // Connection container to make cleanup easier
    private val connectionsContainer = container()

    // For tracking changes
    private var currentHiddenCount = 0
    private var lastNodeInfos = listOf<NodeInfo>()
    private var lastConnectionInfos = listOf<ConnectionInfo>()

    init {
        // Create background
        background = graphics {
            fill(Colors["#222222EE"]) {
                roundRect(0.0, 0.0, baseWidth, baseHeight, 10.0, 10.0)
            }
        }

        // Create title
        text("Neural Network (NEAT)", textSize = 16.0) {
            position(baseWidth / 2, 15)
            centerXOn(this@NetworkVisualizerComponent)
            color = Colors.WHITE
        }

        // Container for info texts at the bottom
        infoTexts = container {
            position(10, 170)

            // Add explanatory labels
            text("Green = positive", textSize = 10.0) {
                position(0, 0)
                color = Colors.GREEN
            }

            text("Red = negative", textSize = 10.0) {
                position(0, 15)
                color = Colors.RED
            }

            nodeCountText = text("Hidden Nodes: 0", textSize = 10.0) {
                position(0, 30)
                color = Colors.YELLOW
            }
        }
    }

    /**
     * Represents a connection between nodes with weight information
     */
    data class ConnectionInfo(
        val fromId: Int,
        val toId: Int,
        val weight: Float = 0f,
        val enabled: Boolean = true
    )

    /**
     * Represents a neural network node with position and type information
     */
    data class NodeInfo(
        val id: Int,
        val type: NodeType,
        val x: Double,
        val y: Double,
        val activationName: String = ""
    )

    /**
     * Node types for NEAT networks
     */
    enum class NodeType { INPUT, HIDDEN, OUTPUT }

    /**
     * Update the network visualization with the current structure and values
     */
    fun updateNetwork(
        nodeInfos: List<NodeInfo>,
        connectionInfos: List<ConnectionInfo>,
        nodeValues: Map<Int, Float>
    ) {
        // Always do a complete rebuild to avoid any stray connections
        rebuildVisualization(nodeInfos, connectionInfos, nodeValues)
    }

    /**
     * Completely rebuilds the visualization from scratch
     */
    private fun rebuildVisualization(
        nodeInfos: List<NodeInfo>,
        connectionInfos: List<ConnectionInfo>,
        nodeValues: Map<Int, Float>
    ) {
        // Update the hidden node count
        val hiddenNodesCount = nodeInfos.count { it.type == NodeType.HIDDEN }
        nodeCountText.text = "Hidden Nodes: $hiddenNodesCount"
        currentHiddenCount = hiddenNodesCount

        // Clear all existing elements
        clearAllElements()

        // Adjust background size if needed
        val newHeight = baseHeight.coerceAtLeast(200 + hiddenNodesCount * 10.0)
        background = graphics {
            fill(Colors["#222222EE"]) {
                roundRect(0.0, 0.0, baseWidth, newHeight, 10.0, 10.0)
            }
        }
        // Ensure background is at the back
        addChildAt(background, 0)

        // Put connections in their own container for easier management
        connectionsContainer.removeFromParent()
        connectionsContainer.removeChildren()
        addChild(connectionsContainer)

        // First create all nodes
        val inputNodes = nodeInfos.filter { it.type == NodeType.INPUT }
        val hiddenNodes = nodeInfos.filter { it.type == NodeType.HIDDEN }
        val outputNodes = nodeInfos.filter { it.type == NodeType.OUTPUT }

        // Create input nodes first (for correct labeling)
        for (nodeInfo in inputNodes) {
            createNode(nodeInfo, nodeValues[nodeInfo.id] ?: 0f, inputNodes.indexOf(nodeInfo))
        }

        // Create hidden nodes
        for (nodeInfo in hiddenNodes) {
            createNode(nodeInfo, nodeValues[nodeInfo.id] ?: 0f, 0)
        }

        // Create output nodes
        for (nodeInfo in outputNodes) {
            createNode(nodeInfo, nodeValues[nodeInfo.id] ?: 0f, 0)
        }

        // Only then create connections between EXISTING nodes
        for (connectionInfo in connectionInfos) {
            // Verify both nodes exist before creating a connection
            if (nodes.containsKey(connectionInfo.fromId) &&
                nodes.containsKey(connectionInfo.toId)
            ) {
                createConnection(connectionInfo, nodeInfos, nodeValues)
            }
        }

        // Store the current structure for future comparison
        lastNodeInfos = nodeInfos.toList()
        lastConnectionInfos = connectionInfos.toList()
    }

    /**
     * Remove all visualization elements
     */
    private fun clearAllElements() {
        // Remove all nodes
        for (node in nodes.values) {
            node.removeFromParent()
        }
        nodes.clear()

        // Remove all value labels
        for (label in valueLabels.values) {
            label.removeFromParent()
        }
        valueLabels.clear()

        // Remove all node descriptions
        for (desc in nodeDescriptions.values) {
            desc.removeFromParent()
        }
        nodeDescriptions.clear()

        // Clear all connection lines
        connectionsContainer.removeChildren()
        connections.clear()

        // Remove the background
        background.removeFromParent()
    }

    /**
     * Create a node in the visualization
     */
    private fun createNode(nodeInfo: NodeInfo, value: Float, index: Int) {
        // Create new node
        val node = circle(
            radius = nodeRadius,
            fill = Colors["#888888"],
            stroke = Colors.WHITE,
            strokeThickness = 2.0
        ) {
            position(nodeInfo.x - nodeRadius, nodeInfo.y - nodeRadius)
        }
        nodes[nodeInfo.id] = node

        // Update the node's fill color based on value
        val nodeColor = if (nodeInfo.type == NodeType.OUTPUT) {
            // Use yellow/green for flap (>0.5) and blue for no-flap
            if (value > 0.5f) {
                // Yellow to green gradient for flap decision
                RGBA((200 + value * 55).toInt(), 255, 0)
            } else {
                // Blue gradient for no-flap decision
                RGBA(100, 100, (200 + (1 - value) * 55).toInt())
            }
        } else {
            // For input and hidden nodes, use green/red
            val brightness = (0.3f + 0.7f * abs(value)).coerceIn(0f, 1f)
            if (value >= 0) {
                RGBA(0, (brightness * 255).toInt(), 0)
            } else {
                RGBA((brightness * 255).toInt(), 0, 0)
            }
        }
        node.fill = nodeColor

        // Create value label
        val valueLabel = text(formatValue(value), textSize = 9.0) {
            position(nodeInfo.x + nodeRadius + 2, nodeInfo.y - 6)
            color = Colors.WHITE
        }
        valueLabels[nodeInfo.id] = valueLabel

        // Create node description
        val description = when (nodeInfo.type) {
            NodeType.INPUT -> {
                val labels = listOf("Bird Y", "Pipe X", "Velocity")
                if (index < labels.size) labels[index] else "Input ${nodeInfo.id}"
            }

            NodeType.HIDDEN -> "H${nodeInfo.id}"
            NodeType.OUTPUT -> "Flap"
        }

        val descriptionColor = when (nodeInfo.type) {
            NodeType.INPUT -> Colors.WHITE
            NodeType.HIDDEN -> Colors.YELLOW
            NodeType.OUTPUT -> Colors.WHITE
        }

        val descriptionLabel = text(description, textSize = 10.0) {
            position(nodeInfo.x - nodeRadius - this.width - 5.0, nodeInfo.y)
            color = descriptionColor
        }
        nodeDescriptions[nodeInfo.id] = descriptionLabel
    }

    /**
     * Create a connection in the visualization
     */
    private fun createConnection(
        connectionInfo: ConnectionInfo,
        nodeInfos: List<NodeInfo>,
        nodeValues: Map<Int, Float>
    ) {
        // Find the nodes this connection links
        val fromNode = nodes[connectionInfo.fromId] ?: return
        val toNode = nodes[connectionInfo.toId] ?: return

        // Get node positions (center of the node)
        val fromNodeInfo = nodeInfos.find { it.id == connectionInfo.fromId } ?: return
        val toNodeInfo = nodeInfos.find { it.id == connectionInfo.toId } ?: return

        // Calculate line color and opacity based on weight and activation
        val fromValue = nodeValues[connectionInfo.fromId] ?: 0f
        val toValue = nodeValues[connectionInfo.toId] ?: 0f

        val lineAlpha = if (connectionInfo.enabled) {
            // More visible when the connection weight has the same sign as the activation
            if ((connectionInfo.weight > 0 && fromValue * toValue > 0) ||
                (connectionInfo.weight < 0 && fromValue * toValue < 0)
            ) {
                0.8
            } else {
                0.3
            }
        } else {
            0.1 // Very faint if disabled
        }

        // Create new connection line - put it in the connections container
        val lineGraphics = connectionsContainer.graphics {
            stroke(
                if (connectionInfo.weight >= 0) Colors.AZURE else Colors.PINK,
                lineWidth = (abs(connectionInfo.weight) * 2.0).coerceIn(0.5, 3.0)
            ) {
                line(fromNodeInfo.x, fromNodeInfo.y, toNodeInfo.x, toNodeInfo.y)
            }
        }
        lineGraphics.alpha = lineAlpha

        connections.add(Pair(lineGraphics, connectionInfo))
    }

    /**
     * Format a float value for display
     */
    private fun formatValue(value: Float): String {
        // Round to 2 decimal places
        val rounded = (value * 100).roundToInt() / 100f
        return if (value >= 0) "+$rounded" else "$rounded"
    }

    /**
     * Simple update for testing - creates a default network visualization
     * This can be used when we don't have access to the actual NEAT structure
     */
    fun updateWithSimulatedNetwork(
        inputs: FloatArray,
        hiddenValues: FloatArray?,
        output: Float
    ) {
        // Create NodeInfo objects for input, hidden, and output nodes
        val nodeInfos = mutableListOf<NodeInfo>()
        val connectionInfos = mutableListOf<ConnectionInfo>()
        val nodeValues = mutableMapOf<Int, Float>()

        // Create input nodes
        for (i in inputs.indices) {
            val id = i + 1
            nodeInfos.add(NodeInfo(id, NodeType.INPUT, inputX, startY + i * 30, "Linear"))
            nodeValues[id] = inputs[i]
        }

        // Create output node
        val outputId = inputs.size + (hiddenValues?.size ?: 0) + 1
        nodeInfos.add(NodeInfo(outputId, NodeType.OUTPUT, outputX, startY + 30, "Sigmoid"))
        nodeValues[outputId] = output

        // Create hidden nodes and connections if we have hidden values
        if (hiddenValues != null && hiddenValues.isNotEmpty()) {
            val numHidden = hiddenValues.size
            val hiddenX = (inputX + outputX) / 2

            // Create hidden nodes positioned in a column between input and output
            val totalHeight = 140.0
            val verticalSpacing = if (numHidden > 1) totalHeight / (numHidden - 1) else 30.0
            val hiddenStartY =
                startY + (30 - verticalSpacing * (numHidden - 1) / 2).coerceAtLeast(0.0)

            for (i in 0 until numHidden) {
                val id = inputs.size + i + 1
                val y = hiddenStartY + (i * verticalSpacing)
                nodeInfos.add(NodeInfo(id, NodeType.HIDDEN, hiddenX, y, "Tanh"))
                nodeValues[id] = hiddenValues[i]

                // Connect each input to this hidden node
                for (inputIdx in inputs.indices) {
                    val inputId = inputIdx + 1
                    connectionInfos.add(
                        ConnectionInfo(
                            fromId = inputId,
                            toId = id,
                            weight = (inputs[inputIdx] * hiddenValues[i]).coerceIn(-1f, 1f)
                        )
                    )
                }

                // Connect this hidden node to the output
                connectionInfos.add(
                    ConnectionInfo(
                        fromId = id,
                        toId = outputId,
                        weight = (hiddenValues[i] * output).coerceIn(-1f, 1f)
                    )
                )
            }
        } else {
            // If no hidden layer, connect inputs directly to output
            for (i in inputs.indices) {
                val inputId = i + 1
                connectionInfos.add(
                    ConnectionInfo(
                        fromId = inputId,
                        toId = outputId,
                        weight = (inputs[i] * output).coerceIn(-1f, 1f)
                    )
                )
            }
        }

        // Update the visualization with our constructed network
        updateNetwork(nodeInfos, connectionInfos, nodeValues)
    }
}
