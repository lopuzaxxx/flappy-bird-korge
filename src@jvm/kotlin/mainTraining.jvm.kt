
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

val gap = 100

class TrainingBird(var brain: Network, val sprite: Sprite) {
    var velocity = 0.0
        set(value) {
            field = value
        }

    var fitness: Float = 0f

    fun think(pipes: List<View>) {
        // Find the closest pipe in front of the bird
        val closestPipe = pipes.filter { it.x + it.width > sprite.x }.minByOrNull { it.x }

        // If no pipe is found, use a default value far away
        val pipeX = closestPipe?.x ?: 1000.0
        val pipeY = closestPipe?.y ?: 256.0

        // Correct and more meaningful inputs
        val inputs = FloatArray(3)

        // 1. Vertical distance to the middle of the pipe opening
        val pipeCenterY = pipeY + (gap / 2.0)
        inputs[0] = ((sprite.y - pipeCenterY) / 256.0).toFloat() // Normalized by half the screen height (256.0)

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

class TrainingSceneJvm : Scene() {

    private var activeBirds = mutableListOf<TrainingBird>()
    private var savedBirds = mutableListOf<TrainingBird>()
    private var pipes = mutableListOf<View>()
    private val populationSize = 100

    private lateinit var scoreText: Text
    private lateinit var generationText: Text
    private lateinit var activeBirdsText: Text
    private lateinit var bluebirdAnimation: SpriteAnimation
    private lateinit var pipeImage: Bitmap
    private lateinit var hitSound: Sound
    private lateinit var pointSound: Sound
    private val gravity = 1000.0

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

        val bluebirdSpriteMap = resourcesVfs["bluebird.png"].readBitmap()
        bluebirdAnimation = SpriteAnimation(
            spriteMap = bluebirdSpriteMap,
            spriteWidth = 34,
            spriteHeight = 24,
            columns = 4,
            rows = 1
        )

        pipeImage = resourcesVfs["pipe-green.png"].readBitmap()
        hitSound = resourcesVfs["hit.wav"].readSound()
        pointSound = resourcesVfs["point.wav"].readSound()

        scoreText = text("Score: 0").xy(10, 10)
        generationText = text("Generation: 1").xy(10, 30)
        activeBirdsText = text("Active Birds: ${activeBirds.size}").xy(10, 50)

        pool.evolve(environment)

        for (i in 0 until populationSize) {
            val bird = TrainingBird(
                instinctInstance.Network(),
                sprite(bluebirdAnimation).position(100, 256)
            )
            activeBirds.add(bird)
        }

        addUpdater { dt ->
            if (activeBirds.isEmpty()) {
                nextGeneration()
            }

            pipes.forEach { it.x -= 2.0 }
            pipes.removeAll { it.x < -64 }

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

                bird.think(pipes)

                var collision = false
                for (pipe in pipes) {
                    if (bird.sprite.collidesWith(pipe)) {
                        collision = true
                    }
                }
                if (collision || bird.sprite.y >= 375) {
                    activeBirds.remove(bird)
                    savedBirds.add(bird)
                    bird.sprite.removeFromParent()
                }
            }

            scoreText.text = "Score: ${activeBirds.maxOfOrNull { it.fitness }}"
            activeBirdsText.text = "Active Birds: ${activeBirds.size}"
        }

        addFixedUpdater(time = 1.0.seconds) {
            val middle = Random.nextDouble(175.0, 295.0)
            val topPipe = image(pipeImage) {
                scaleY = -1.0
                anchor(0.5, 0.0)
                position(512, middle - gap / 2)
            }
            val bottomPipe = image(pipeImage) {
                anchor(0.5, 0.0)
                position(512, middle + gap / 2)
            }
            pipes.add(topPipe)
            pipes.add(bottomPipe)
        }
    }

    private fun Container.nextGeneration() {
        generationText.text = "Generation: ${pool.generation}"
        scoreText.text = "Score: 0"

        pool.evolve(environment)

        activeBirds.addAll(savedBirds)
        savedBirds.clear()

        activeBirds.forEachIndexed { index, bird ->
            bird.sprite.x = 100.0
            bird.sprite.y = 256.0
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
