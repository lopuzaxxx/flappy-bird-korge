
// Use local imports since Network is only available in JVM module
import eu.timerertim.knevo.*
import eu.timerertim.knevo.activation.*
import eu.timerertim.knevo.instinct.*
import eu.timerertim.knevo.selection.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.math.*

class TrainingBird(var brain: Network, val sprite: Sprite) {
    var velocity = 0.0
        set(value) {
            field = value
        }

    var fitness: Float = 0f

    // Store last network inputs and outputs for visualization
    var lastThinkInputs: FloatArray = FloatArray(3)
    var lastThinkOutput: Float = 0f

    // For hidden layer visualization (estimated since we don't have direct access)
    var lastHiddenLayerValues: FloatArray = FloatArray(4) { 0f }

    // Helper method to estimate hidden layer activations with meaningful specializations
    private fun estimateHiddenLayerValues(inputs: FloatArray, output: Float): FloatArray {
        // This is a simulation of specialized hidden neurons that perform specific functions
        val hiddenValues = FloatArray(4)

        // H1: "Height Detector" - Sensitive to bird's vertical position relative to pipe
        hiddenValues[0] = inputs[0] * 1.5f

        // H2: "Distance Analyzer" - Evaluates horizontal distance to pipe
        hiddenValues[1] = (1.0f - abs(inputs[1])) * 1.2f

        // H3: "Velocity Monitor" - Tracks if bird is rising or falling
        hiddenValues[2] =
            -inputs[2] * 1.3f  // Negative velocity (rising) produces positive activation

        // H4: "Flap Decision" - Combines all inputs with a bias towards flapping when needed
        // Strongly activated when bird is below pipe center and falling
        hiddenValues[3] = (inputs[0] * 0.6f + inputs[1] * 0.2f - inputs[2] * 0.8f) *
            (if (output > 0.5f) 1.5f else -0.5f)

        // Apply hyperbolic tangent activation to simulate neural network behavior
        for (i in hiddenValues.indices) {
            // Approximate tanh function: constrain values between -1 and 1 with a smooth curve
            hiddenValues[i] =
                (hiddenValues[i] / (1.0f + abs(hiddenValues[i])) * 2.0f).coerceIn(-1.0f, 1.0f)
        }

        return hiddenValues
    }

    fun think(pipes: List<View>, gap: Int) {
        // Find the closest pipe in front of the bird
        val closestPipe = pipes.filter { it.x + it.width > sprite.x }.minByOrNull { it.x }

        // If no pipe is found, use a default value far away
        val pipeX = closestPipe?.x ?: 1000.0
        val pipeY = closestPipe?.y ?: 256.0

        // Correct and more meaningful inputs
        val inputs = FloatArray(3)

        // 1. Vertical distance to the middle of the pipe opening
        val pipeCenterY = pipeY + (gap / 2.0)
        inputs[0] =
            ((sprite.y - pipeCenterY) / 256.0).toFloat() // Normalized by half the screen height (256.0)

        // 2. Horizontal distance to the pipe
        inputs[1] = ((pipeX - sprite.x) / 512.0).toFloat() // Normalized by screen width

        // 3. Current bird velocity
        inputs[2] = (velocity / 200.0).toFloat() // Normalized by a larger possible velocity

        val output = brain.invoke(inputs)

        // Store for network visualization
        lastThinkInputs = inputs.clone()
        lastThinkOutput = output[0]

        // Estimate hidden layer values based on inputs and output
        lastHiddenLayerValues = estimateHiddenLayerValues(inputs, output[0])

        println("flap output: ${output.get(0)}, inputs: ${inputs.joinToString(separator = ", ")}")

        if (output[0] > 0.5) {
            // Perform flap
            velocity = -150.0
        }
    }

}

class TrainingSceneJvm : BaseFlappyBirdScene() {

    private var activeBirds = mutableListOf<TrainingBird>()
    private var savedBirds = mutableListOf<TrainingBird>()
    private val populationSize = 100
    private var visualScore = 0
    private val passedPipes = mutableSetOf<View>()

    private lateinit var scoreText: Text
    private lateinit var generationText: Text
    private lateinit var activeBirdsText: Text
    private lateinit var visualScoreText: Text

    // Neural network visualization
    private lateinit var networkVisualizer: NetworkVisualizerComponent
    private var lastInputs = FloatArray(3) { 0f }
    private var lastOutput = 0f

    val environment = object : Environment<Network> {

        override suspend fun evaluateFitness(population: List<Network>) {
            savedBirds.forEach {
                it.brain.fitness = it.fitness
            }
        }

    }

    val pool = Pool(
        populationSize = populationSize,
        select = Tournament(10)
    )

    val instinctInstance = InstinctInstance(
        inputs = 3,
        outputs = 1,
        outputActivations = listOf(Tanh())
    )

    override suspend fun SContainer.sceneMain() {
        setBackground()

        // Initialize shared resources from base class
        initResources()

        // Initialize ground
        initGround()

        scoreText = text("Fitness: 0").xy(10, 10)
        generationText = text("Generation: 1").xy(10, 30)
        activeBirdsText = text("Active Birds: ${activeBirds.size}").xy(10, 50)
        visualScoreText = text("Score: 0").xy(10, 70)

        // Add neural network visualization
        networkVisualizer = NetworkVisualizerComponent().apply {
            position(250, 100) // Position in the top-right for better visibility
            scale = 0.9
        }
        addChild(networkVisualizer)

        pool.evolve(environment)

        for (i in 0 until populationSize) {
            val bird = TrainingBird(
                instinctInstance.Network(),
                sprite(bluebirdAnimation).position(birdStartX, birdStartY)
            )
            activeBirds.add(bird)
        }

        addUpdater { dt ->
            if (activeBirds.isEmpty()) {
                nextGeneration()
            }

            // Use base class function to update pipes
            updatePipes()

            // Check for passed pipes and update visual score
            if (activeBirds.isNotEmpty()) {
                val bestBird = activeBirds.first() // Use the first bird to track pipe passing
                for (i in pipes.indices step 2) {
                    val topPipe = pipes[i]
                    if (topPipe.x + topPipe.width < bestBird.sprite.x && !passedPipes.contains(
                            topPipe
                        )
                    ) {
                        launch { pointSound.play() }
                        visualScore++
                        visualScoreText.text = "Score: $visualScore"
                        passedPipes.add(topPipe)
                    }
                }
                passedPipes.removeAll { it.x < -64 }
            }

            for (bird in activeBirds.toList()) {
                bird.fitness += 1f
                bird.velocity += gravity * dt.seconds
                bird.sprite.y += bird.velocity * dt.seconds

                if (bird.sprite.y > 375) {
                    bird.sprite.y = 375.0
                    bird.velocity = 0.0
                }

                if (bird.sprite.y < 0) {
                    bird.sprite.y = 0.0
                }

                bird.think(pipes, gap)

                // Use base class function to check collision
                val collision = checkPipeCollision(bird.sprite)
                if (collision || bird.sprite.y >= 375) {
                    activeBirds.remove(bird)
                    savedBirds.add(bird)
                    bird.sprite.removeFromParent()
                }
            }

            scoreText.text = "Fitness: ${activeBirds.maxOfOrNull { it.fitness }}"
            activeBirdsText.text = "Active Birds: ${activeBirds.size}"

            // Update neural network visualization if there are active birds
            if (activeBirds.isNotEmpty()) {
                val bestBird = activeBirds.first()

                // Try to extract the actual network structure from the NEAT network
                val networkStructure = NetworkExtractor.tryExtractNetwork(
                    bestBird.brain,
                    bestBird.lastThinkInputs,
                    floatArrayOf(bestBird.lastThinkOutput)
                )

                // If we could extract the structure, update with it
                if (networkStructure != null) {
                    networkVisualizer.updateNetwork(
                        networkStructure.nodes,
                        networkStructure.connections,
                        networkStructure.nodeValues
                    )
                } else {
                    // Fall back to the simulated network visualization
                    networkVisualizer.updateWithSimulatedNetwork(
                        bestBird.lastThinkInputs,
                        bestBird.lastHiddenLayerValues,
                        bestBird.lastThinkOutput
                    )
                }
            }

            // Keep the ground on top
            bringGroundToTop()
            scoreText.bringToTop()
            generationText.bringToTop()
            activeBirdsText.bringToTop()
            visualScoreText.bringToTop()
        }

        // Use base class pipe spawner function
        setupPipeSpawner { middle ->
            createPipePair(512.0, middle)
        }
    }

    private fun Container.nextGeneration() {
        generationText.text = "Generation: ${pool.generation}"
        scoreText.text = "Fitness: 0"
        visualScore = 0
        visualScoreText.text = "Score: 0"
        passedPipes.clear()

        pool.evolve(environment)

        activeBirds.addAll(savedBirds)
        savedBirds.clear()

        activeBirds.forEachIndexed { index, bird ->
            bird.sprite.x = birdStartX
            bird.sprite.y = birdStartY
            bird.fitness = 0f
            bird.brain = pool.get(index)
            addChild(bird.sprite)
        }

        pipes.forEach {
            it.removeFromParent()
        }

        pipes.clear()
    }

}
