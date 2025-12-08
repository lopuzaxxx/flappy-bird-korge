//
//import ai.nevo.*
//import ai.nevo.activation.*
//import ai.nevo.instinct.*
//import ai.nevo.selection.*
//import korlibs.audio.sound.*
//import korlibs.image.bitmap.*
//import korlibs.image.format.*
//import korlibs.io.file.std.*
//import korlibs.korge.scene.*
//import korlibs.korge.view.*
//import korlibs.korge.view.collision.*
//import korlibs.time.*
//import kotlinx.datetime.*
//import kotlin.random.*
//import kotlin.random.Random
//
//class TrainingBird(val brain: Network, val sprite: Sprite) {
//    var velocity = 0.0
//        set(value) {
////            println("setting velocity: $velocity, time: ${Clock.System.now()}")
//            field = value
//        }
//
////    var score: Double
//
//
//    var fitness: Double
//        get() = brain.fitness
//        set(value) {
//            brain.fitness = value
//        }
//
//    fun think(pipes: List<View>) {
//        val closest = pipes.firstOrNull { it.x + it.width > sprite.x }
//        if (closest != null) {
//            val inputs = DoubleArray(5)
//            inputs[0] = sprite.y / 512.0
//            inputs[1] = velocity / 10.0
//            inputs[2] = closest.x / 512.0
//            inputs[3] = (closest.y - 150) / 512.0
//            inputs[4] = (closest.y + 150) / 512.0
//
//            val output = brain.invoke(inputs)
//
//            println("flap output: ${output.get(0)}")
//
//            if (output[0] > 0.5) {
//                //perform flap
//                velocity = -150.0
//            }
//        }
//    }
//
//    fun mutate() {
//        brain.mutate()
//    }
//
//}
//
//class TrainingScene : Scene() {
//
//    private var activeBirds = mutableListOf<TrainingBird>()
//    private var savedBirds = mutableListOf<TrainingBird>()
//    private var pipes = mutableListOf<View>()
//    private var generation = 0
//    private val populationSize = 100
//
//    private lateinit var scoreText: Text
//    private lateinit var generationText: Text
//    private lateinit var activeBirdsText: Text
//    private lateinit var bluebirdAnimation: SpriteAnimation
//    private lateinit var pipeImage: Bitmap
//    private lateinit var hitSound: Sound
//    private lateinit var pointSound: Sound
//    private val gravity = 1000.0
//
//    val environment = object : Environment<Network> {
//
//        override suspend fun evaluateFitness(population: List<Network>) {
//            population.forEach {
//
//            }
//        }
//
//    }
//
//    val pool = Pool(
//        populationSize = populationSize,
//        select = Tournament(10)
//    )
//
//    val instinctInstance = InstinctInstance(
//        inputs = 5,
//        outputs = 1,
//        outputActivations = listOf(Tanh(slope = 1.0))
//    )
//
//    override suspend fun SContainer.sceneMain() {
//        setBackground()
//
//        val bluebirdSpriteMap = resourcesVfs["bluebird.png"].readBitmap()
//        bluebirdAnimation = SpriteAnimation(
//            spriteMap = bluebirdSpriteMap,
//            spriteWidth = 34,
//            spriteHeight = 24,
//            columns = 4,
//            rows = 1
//        )
//
//        pipeImage = resourcesVfs["pipe-green.png"].readBitmap()
//        hitSound = resourcesVfs["hit.wav"].readSound()
//        pointSound = resourcesVfs["point.wav"].readSound()
//
//        scoreText = text("Score: 0").xy(10, 10)
//        generationText = text("Generation: 1").xy(10, 30)
//        activeBirdsText = text("Active Birds: ${activeBirds.size}").xy(10, 50)
//
//        pool.evolve(environment)
//
//        for (i in 0 until populationSize) {
//            val bird = TrainingBird(
//                instinctInstance.Network(),
//                sprite(bluebirdAnimation).position(100, 256)
//            )
//            activeBirds.add(bird)
//        }
//
//        addUpdater { dt ->
//            if (activeBirds.isEmpty()) {
//                nextGeneration()
//            }
//
//            pipes.forEach { it.x -= 2.0 }
//            pipes.removeAll { it.x < -64 }
//
//            for (bird in activeBirds.toList()) {
//                bird.fitness += 1.0
//                bird.velocity += gravity * dt.seconds
//                bird.sprite.y += bird.velocity * dt.seconds
//
//                if (bird.sprite.y > 375) {
//                    bird.sprite.y = 375.0
//                    bird.velocity = 0.0
//                }
//
//                if (bird.sprite.y < 0) {
//                    bird.sprite.y = 0.0
//                }
//
//                var collision = false
//                for (pipe in pipes) {
//                    if (bird.sprite.collidesWith(pipe)) {
//                        collision = true
//                    }
//                }
//                if (collision || bird.sprite.y >= 375) {
//                    activeBirds.remove(bird)
//                    savedBirds.add(bird)
//                    bird.sprite.removeFromParent()
//                }
//            }
//
//            scoreText.text = "Score: ${activeBirds.maxOfOrNull { it.fitness }}"
//
////            scoreText.text = "Score: ${savedBirds.maxOfOrNull { it.fitness } ?: 0}"
//            activeBirdsText.text = "Active Birds: ${activeBirds.size}"
//        }
//
//        addFixedUpdater(time = 1.0.seconds) {
//            val gap = 100
//            val middle = Random.nextDouble(175.0, 295.0)
//            val topPipe = image(pipeImage) {
//                scaleY = -1.0
//                anchor(0.5, 0.0)
//                position(512, middle - gap / 2)
//            }
//            val bottomPipe = image(pipeImage) {
//                anchor(0.5, 0.0)
//                position(512, middle + gap / 2)
//            }
//            pipes.add(topPipe)
//            pipes.add(bottomPipe)
//        }
//
//        addFixedUpdater(time = 0.2.seconds) {
//            activeBirds.forEach {
//                it.think(pipes)
//            }
//        }
//    }
//
//    private fun Container.nextGeneration() {
//        generation++
//        generationText.text = "Generation: $generation"
//        scoreText.text = "Score: 0"
//
//        activeBirds.addAll(savedBirds)
//        savedBirds.clear()
//
//        pool.evolve(environment)
////        for (i in 0 until totalPopulation) {
////            val bird = pickOne()
////            bird.sprite = sprite(bluebirdAnimation).position(100, 256)
////            activeBirds.add(bird)
////        }
////        savedBirds.clear()
//
//        activeBirds.forEach {
//            it.sprite.x = 100.0
//            it.sprite.y = 256.0
//            it.fitness = 0.0
//            addChild(it.sprite)
//        }
//
//        pipes.forEach {
//            it.removeFromParent()
//        }
//
//        pipes.clear()
//    }
//
////    private fun pickOne(): TrainingBird {
////        var index = 0
////        var r = Random.nextDouble()
////        while (r > 0) {
////            r -= savedBirds[index].score
////            index++
////        }
////        index--
////        val bird = savedBirds[index]
////        val child = TrainingBird(bird.brain)
////        child.mutate()
////        return child
////    }
//}
