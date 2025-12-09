
import eu.timerertim.knevo.*
import eu.timerertim.knevo.activation.*
import eu.timerertim.knevo.instinct.*
import eu.timerertim.knevo.selection.*
import korlibs.audio.sound.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.collision.*
import korlibs.time.*
import kotlin.random.Random
import kotlinx.coroutines.launch

class TrainingBird(var brain: Network, val sprite: Sprite) {
    var velocity = 0.0
        set(value) {
            field = value
        }

    var fitness: Float = 0f

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
        outputActivations = listOf(Tanh()),

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
